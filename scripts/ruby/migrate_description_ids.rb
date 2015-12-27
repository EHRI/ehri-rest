#!/usr/bin/env jruby

# Update descriptions to have a scoped id rather than a random one.

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

module Ehri
  module Fixer
    include Ehri

    @total = 0

    def self.fix_type(cls, java_cls)
      serializer = Persistence::Serializer.new(Graph)
      Manager.get_frames(cls, java_cls).each do |desc|
        bundle = serializer.vertex_frame_to_bundle(desc)
        parent = Graph.frame(desc.get_entity.as_vertex, Java::EuEhriProjectModelsBase::PermissionScope.java_class)
        newid = cls.get_idgen.generate_id(parent, bundle)
        puts "New ID: #{newid}"
        desc.as_vertex.set_property("__id", newid)
        @total += 1
      end
    end

    begin
      fix_type(EntityClass::REPOSITORY_DESCRIPTION, Models::RepositoryDescription.java_class)
      fix_type(EntityClass::DOCUMENT_DESCRIPTION, Models::DocumentDescription.java_class)
      fix_type(EntityClass::HISTORICAL_AGENT_DESCRIPTION, Models::HistoricalAgentDescription.java_class)
      fix_type(EntityClass::CVOC_CONCEPT_DESCRIPTION, Java::EuEhriProjectModelsCvoc::ConceptDescription.java_class)
    rescue Exception => msg
      puts "ERROR - #{msg}"
    end

    Core::GraphReindexer.new(Graph).reindex("entities")

    Graph.get_base_graph.commit
    puts "Committed: #{@total}"
    Graph.get_base_graph.shutdown

  end
end
