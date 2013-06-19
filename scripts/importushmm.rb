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
java_import "eu.ehri.project.persistance.ActionManager"
java_import "eu.ehri.project.definitions.EventTypes"
java_import "eu.ehri.project.importers.IcaAtomEadImporter"
java_import "eu.ehri.project.importers.IcaAtomEadHandler"
java_import "eu.ehri.project.importers.ImportLog"

# Use the default if NEO4J_DB isn't set...
DB_PATH = ENV['NEO4J_DB'] ||= "#{ENV['NEO4J_HOME']}/data/graph.db"

# Currently using a hard-coded path and repository id
USER_ID = "mike"
REPO_ID = "us-005578"
DATA_DIR = "ead"

COMMIT_MAX = 2500

# Initialise a graph and the manager.
# Note: the graph must not be being used elsewhere (i.e. by the server)
graph = FramedGraph.new(Neo4jGraph.new DB_PATH)
manager = GraphManagerFactory.get_instance graph

# Go through the system events looking for ones that don't have a type.
# Then try and guess/infer the correct type from the string contents of
# the log message. Nasty, but effective.
def import_with_scope(xmlpath, graph, scope, event, log)
    puts "Importing #{xmlpath}"
    child_path = File.join DATA_DIR, xmlpath.match(/irn(?<num>\d+)\.xml$/)["num"]

    children = []
    if Dir.exists? child_path
        children = Dir.glob("#{child_path}/irn*xml")
    end


    importer = IcaAtomEadImporter.new(graph, scope, log)

    importer.add_creation_callback do |item|
        puts "Created item: #{item.get_id}"
        event.add_subjects item
        log.add_created

        if log.get_successful > 0 and log.get_successful % COMMIT_MAX == 0
            graph.get_base_graph.commit
        end

        children.each do |cxml|
            import_with_scope(cxml, graph, item, event, log)
        end
    end

    importer.add_update_callback do |item|
        puts "Updated item: #{item.get_id}"
        event.add_subjects item
        log.add_updated

        if log.get_successful > 0 and log.get_successful % COMMIT_MAX == 0
            graph.get_base_graph.commit
        end

        children.each do |cxml|
            import_with_scope(cxml, graph, item, event, log)
        end
    end

    handler = IcaAtomEadHandler.new importer
    spf = SAXParserFactory.new_instance
    spf.set_namespace_aware false
    spf.set_validating false
    spf.set_schema nil
    parser = spf.new_sax_parser

    File.open(xmlpath, "r") do |f|
        parser.parse(f.to_inputstream, handler)
    end
end


begin

    # lookup USHMM
    ushmm = manager.get_frame(REPO_ID, Repository.java_class)
    user = manager.get_frame(USER_ID, UserProfile.java_class)

    # Start an action!
    ctx = ActionManager.new(graph, ushmm).log_event(user, EventTypes::ingest, "Importing USHMM data")
    log = ImportLog.new(ctx)

    # We basically need recursive behaviour here
    Dir.glob("#{DATA_DIR}/irn*xml").each do |xmlpath|
        import_with_scope xmlpath, graph, ushmm, ctx, log
    end

    puts "Updated: #{log.get_updated}"
    puts "Created: #{log.get_created}"    

    graph.get_base_graph.commit
    puts "Commited"
rescue
    # Oops!
    graph.get_base_graph.rollback
    raise
end


