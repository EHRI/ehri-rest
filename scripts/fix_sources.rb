#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "ruby", "lib")
require "#{File.join(File.dirname(__FILE__), "ruby", "lib", "ehri")}"

module Ehri
  module Fixer
    include Ehri
    Manager.get_entities(EntityClass::REPOSITORY_DESCRIPTION, Models::RepositoryDescription.java_class).each do |repodesc|
      begin
        main = repodesc.as_vertex.get_property("maintenanceNotes")
        if main =~ /Description sources: (.+)/
          repodesc.as_vertex.set_property("sources", $1)
        end
      rescue Exception => msg
        puts "ERROR #{event} - #{msg}"
      end
    end
    Graph.get_base_graph.commit
  end
end
