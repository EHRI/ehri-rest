#!/usr/bin/env jruby

# For debugging purposes we need some descriptive properties
# on 'link' nodes, which only serve to connect a set of other
# nodes together. Unfortunately the same

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

    java_import "eu.ehri.project.definitions.Ontology"
    java_import "eu.ehri.project.definitions.Entities"
    java_import "eu.ehri.project.persistence.ActionManager"
    java_import "eu.ehri.project.models.annotations.EntityType"
    java_import "com.tinkerpop.blueprints.Direction"

    def self.get_properties(vertex)
      hash = {}
      vertex.get_property_keys.each do |k|
        hash[k] = vertex.get_property k
      end
      hash
    end

    def self.is_system_event(node)
        node.get_property(EntityType::TYPE_KEY) == Entities::SYSTEM_EVENT
    end

    # Check the event hierarchy

    Manager.get_frames(EntityClass::SYSTEM_EVENT, Events::SystemEvent.java_class).each do |event|
        vertex = event.as_vertex
        vertex.get_vertices(Direction::IN, Ontology::ENTITY_HAS_EVENT).each do |link|
            puts "Stamping #{link}"
            link.set_property ActionManager::DEBUG_TYPE, ActionManager::EVENT_LINK
            link.set_property ActionManager::LINK_TYPE, Ontology::ENTITY_HAS_LIFECYCLE_EVENT
        end
        vertex.get_vertices(Direction::OUT, Ontology::EVENT_HAS_ACTIONER).each do |link|
            puts "Stamping #{link}"
            link.set_property ActionManager::DEBUG_TYPE, ActionManager::EVENT_LINK
            link.set_property ActionManager::LINK_TYPE, Ontology::ACTIONER_HAS_LIFECYCLE_ACTION
        end
    end

    Graph.get_base_graph.shutdown
  end
end
