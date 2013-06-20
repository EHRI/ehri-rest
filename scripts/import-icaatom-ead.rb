#!/usr/bin/env jruby

# Abort if we don't have NEO4J_HOME
if ENV['NEO4J_HOME'].nil? or ENV['NEO4J_HOME'].empty? then
    abort "Error: NEO4J_HOME environment variable must be defined."
end

# Abort if we don't have NEO4J_HOME
if ENV['CLASSPATH'].nil? or ENV['CLASSPATH'].empty? then
    abort "Error: CLASSPATH environment variable must be defined."
end

# The magic necessary to do Java stuff...
require "java"

# Import Java classes like so...
java_import "javax.xml.parsers.SAXParserFactory"
java_import "com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph"
java_import "com.tinkerpop.frames.FramedGraph"
java_import "eu.ehri.project.core.GraphManagerFactory"
java_import "eu.ehri.project.models.EntityClass"
java_import "eu.ehri.project.models.base.Frame"
java_import "eu.ehri.project.models.Repository"
java_import "eu.ehri.project.models.UserProfile"
java_import "eu.ehri.project.views.impl.LoggingCrudViews"
java_import "eu.ehri.project.persistance.ActionManager"
java_import "eu.ehri.project.persistance.Bundle"
java_import "eu.ehri.project.persistance.BundleDAO"
java_import "eu.ehri.project.definitions.EventTypes"
java_import "eu.ehri.project.importers.IcaAtomEadImporter"
java_import "eu.ehri.project.importers.IcaAtomEadHandler"
java_import "eu.ehri.project.importers.SaxImportManager"
java_import "eu.ehri.project.importers.ImportLog"
java_import "com.google.common.base.Optional"

# Use the default if NEO4J_DB isn't set...
DB_PATH = ENV['NEO4J_DB'] ||= "#{ENV['NEO4J_HOME']}/data/graph.db"

# Currently using a hard coded user and dir
USER_ID = "mike"
DATA_DIR = "icaatom-export"

COMMIT_MAX = 2500

# Initialise a graph and the manager.
# Note: the graph must not be being used elsewhere (i.e. by the server)
graph = FramedGraph.new(Neo4jGraph.new DB_PATH)
manager = GraphManagerFactory.get_instance graph


def import_repo_data(graph, manager, dir, user)

    repo_id = File.basename(dir)
    repo = manager.get_frame(repo_id, Repository.java_class)
    ead_files = Dir.glob("#{dir}/*.xml")

    msg = "Importing ICA-Atom EAD for repository #{repo_id}"

    manager = SaxImportManager.new(graph, repo, user, IcaAtomEadImporter.java_class, IcaAtomEadHandler.java_class)
    log = manager.import_files(ead_files, msg)

    puts "Done EAD import for #{repo_id}: created: #{log.get_created}, updated: #{log.get_updated}"
end

begin

    # lookup USHMM
    user = manager.get_frame(USER_ID, UserProfile.java_class)

    # We basically need recursive behaviour here
    Dir.glob("#{DATA_DIR}/??-??????").sort.each do |dir|
        import_repo_data(graph, manager, dir, user)
    end

    graph.get_base_graph.commit
    puts "Committed"
rescue
    # Oops!
    graph.get_base_graph.rollback
    raise
end


