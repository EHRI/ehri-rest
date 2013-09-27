#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

USER = ARGV.shift
CSV = ARGV.shift

if USER.nil? or CSV.nil?
  $stderr.puts("usage: prog <user> <csv-file>")
  exit(1)
end

module Ehri
  module Fixer
    include Ehri

    java_import "au.com.bytecode.opencsv.CSVReader"

    data = Hash.new

    user = Manager.get_frame(USER, Models::UserProfile.java_class)

    # Note gotcah on char!
    csv = CSVReader.new(Java::JavaIo::FileReader.new(CSV), ","[0].ord.to_java(:char))

    ctx = Persistance::ActionManager.new(Graph).log_event(user, EventTypes::modification,
                      "Correcting archivist's note field missed from ICA-Atom migration")

    while (row = csv.read_next) != nil
      arr = row.to_a
      data[arr[0]] = arr[1]
    end

    matched = 0
    errors = 0

    Manager.get_frames(EntityClass::DOCUMENTARY_UNIT, Models::DocumentaryUnit.java_class).each do |doc|
      begin
        desc = doc.get_descriptions().iterator().next()
        if data.has_key?(doc.get_identifier)
          ctx.create_version(doc)
          ctx.add_subjects(doc)
          matched += 1
          desc.as_vertex.set_property("archivistNote", data[doc.get_identifier])
        end
        #puts desc.get_name
      rescue Exception => msg
        puts "ERROR #{msg}"
        errors += 1
      end
    end
    if errors == 0
        Graph.get_base_graph.commit
        puts "Size: #{data.size}, matched: #{matched} - committed"
    else
        Graph.get_base_graph.rollback
        puts "Size: #{data.size}, matched: #{matched}, errors: #{errors} - rolled back"
    end
  end
end
