#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

module Ehri
  module Fixer
    include Ehri

    module Events
      include_package "eu.ehri.project.models.events"
    end
    module ModelsBase
      include_package "eu.ehri.project.models.base"
    end

    # Set a _childCount property on system events for how many subjects they have
    total = 0
    Manager.get_frames(EntityClass::SYSTEM_EVENT, Events::SystemEvent.java_class).each do |evt|
      begin
        count = evt.get_subjects.iterator.count
        evt.as_vertex.set_property(ModelsBase::ItemHolder::CHILD_COUNT, count)
        total += 1
      rescue Exception => msg
        puts "ERROR - #{msg}"
      end
    end
    Graph.get_base_graph.commit
    puts "Committed: #{total}"
    Graph.get_base_graph.shutdown
  end
end
