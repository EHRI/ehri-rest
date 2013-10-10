#!/usr/bin/env jruby

# Fix issue database before anyone actually starts using it.
# Some repositories were initially imported without a correct
# permission scope (should have been their country node.)
# This led to doc global ids being calculated without the
# correct scope prefix. This script adds the missing country
# scope where necessary, recalculates ids for doc units, and
# rebuilds the index.

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

USER = ARGV.shift


module Ehri
  module Fixer
    include Ehri

    java_import "au.com.bytecode.opencsv.CSVReader"

    serializer = Persistence::Serializer.new(Graph)
    reindexer = Core::GraphReindexer.new(Graph)

    Manager.get_frames(EntityClass::REPOSITORY, Models::Repository.java_class).each do |repo|
      begin
        country = repo.get_country.iterator.next
        scope = repo.get_permission_scope
        if scope != country
          puts "Repo #{repo.get_id}: Country: #{country} != Scope: #{scope}"
          repo.set_permission_scope(country)
        end
      rescue Exception => msg
        raise msg
      end
    end
    Graph.get_base_graph.commit

    errors = 0

    Manager.get_frames(EntityClass::DOCUMENTARY_UNIT, Models::DocumentaryUnit.java_class).each do |doc|
      begin
        cls = EntityClass::DOCUMENTARY_UNIT
        idgen = cls.getIdgen
        bundle = serializer.vertex_frame_to_bundle(doc)
        scope = doc.get_permission_scope
        newid = idgen.generate_id(cls, scope, bundle)
        if newid != doc.get_id
          puts "ID diff: #{doc.get_id} -> #{newid}"
          doc.as_vertex.set_property("__ID__", newid)
        end
      rescue Exception => msg
        puts "ERROR #{msg}"
        errors += 1
      end
    end
    if errors == 0
        Graph.get_base_graph.commit
        puts "Reindexing..."
        reindexer.reindex("entities")
        Graph.get_base_graph.commit
        puts "Committed..."
    else
        Graph.get_base_graph.rollback
        puts "Size: #{data.size}, matched: #{matched}, errors: #{errors} - rolled back"
    end
  end
end
