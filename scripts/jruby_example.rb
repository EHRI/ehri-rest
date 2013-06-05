#!/usr/bin/env jruby

# NB: Things to do before running this script:
#  - make sure jruby is on the PATH
#  - make sure the CLASSPATH env var is set for the given NEO4J
#  - make sure the NEO4J_HOME env var is set

# The magic necessary to do Java stuff...
require "java"

# Import Java classes like so...
java_import "com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph"
java_import "com.tinkerpop.frames.FramedGraph"
java_import "eu.ehri.project.core.GraphManagerFactory"
java_import "eu.ehri.project.models.EntityClass"
java_import "eu.ehri.project.models.base.Frame"
java_import "eu.ehri.project.models.events.SystemEvent"
java_import "eu.ehri.project.persistance.Bundle"
java_import "eu.ehri.project.persistance.BundleDAO"
java_import "eu.ehri.project.persistance.Serializer"

# Abort if we don't have NEO4J_HOME
if ENV['NEO4J_HOME'].empty? then
    abort "NEO4J_HOME environment variable must be defined."
end

# Use the default if NEO4J_DB isn't set...
DB_PATH = ENV['NEO4J_DB'] ||= "#{ENV['NEO4J_HOME']}/data/graph.db"

# Wildcard import of a package like so. Note, the stuff
# imported will be used with the given module namespace, i.e.
# EhriModels::DocumentaryUnit
module EhriModels
    include_package "eu.ehri.project.models"
end

# Initialise a graph and the manager.
# Note: the graph must not be being used elsewhere (i.e. by the server)
graph = FramedGraph.new(Neo4jGraph.new DB_PATH)
manager = GraphManagerFactory.get_instance graph

# Test the serializer. Note that the CamelCase Java methods
# can be converted to the snake_case Ruby style automatically.
serializer = Serializer.new graph
ba = manager.get_frame "ba", Frame.java_class
puts serializer.vertex_frame_to_bundle ba

# List a bunch of repositories...
manager.get_frames(EntityClass::REPOSITORY, EhriModels::Repository.java_class).each { |i|    
    puts i.get_id
}

# List the types of events and their log message...
manager.get_frames(EntityClass::SYSTEM_EVENT, SystemEvent.java_class).each { |i|    
    bundle = serializer.vertex_frame_to_bundle(i)
    type = i.action_type
    msg = i.log_message
    puts type, msg
}
