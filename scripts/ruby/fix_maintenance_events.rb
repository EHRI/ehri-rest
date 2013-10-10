#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

module Ehri
  module Fixer
    include Ehri


    Manager.get_frames(EntityClass::REPOSITORY_DESCRIPTION, Models::RepositoryDescription.java_class).each do |repodesc|
      begin
        notes = repodesc.as_vertex.get_property("maintenanceNotes")
        if notes =~ /ICA-AtoM identifier/ and not repodesc.get_maintenance_events.iterator.has_next
            sources = repodesc.as_vertex.get_property("sources")
            if sources.class == Java::JavaUtil::ArrayList
                sources = sources.to_a.join(", ")
            elsif sources.nil?
                sources = ""
            end

            persister = Persistence::BundleDAO.new(Graph)
            ev1 = Persistence::Bundle.new(EntityClass::MAINTENANCE_EVENT, {
                "maintenanceEvent/source" => sources,
                "eventType" => "creation",
                "maintenanceEvent/date" => "2012-03-09"
            })
        
            ev2 = Persistence::Bundle.new(EntityClass::MAINTENANCE_EVENT, {
                "maintenanceEvent/source" => "Imported from EHRI spreadsheet",
                "eventType" => "creation",
                "maintenanceEvent/date" => "2012-03-09"
            })
        
            ev3 = Persistence::Bundle.new(EntityClass::MAINTENANCE_EVENT, {
                "maintenanceEvent/source" => "Exported from ICA-AtoM",
                "eventType" => "creation",
                "maintenanceEvent/date" => "2013-09-09"
            })
            m1 = persister.create(ev1, Models::MaintenanceEvent.java_class)
            m2 = persister.create(ev2, Models::MaintenanceEvent.java_class)
            m3 = persister.create(ev3, Models::MaintenanceEvent.java_class)
            
            repodesc.add_maintenance_event(m1)
            repodesc.add_maintenance_event(m2)
            repodesc.add_maintenance_event(m3)

            puts "Restored events for: #{repodesc.get_entity.get_id} - #{sources}"
        end
      rescue Exception => msg
        puts "ERROR - #{msg}"
      end
    end
    Graph.get_base_graph.commit
    puts "Committed."
  end
end
