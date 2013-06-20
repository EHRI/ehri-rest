#!/usr/bin/env ruby

require "#{File.dirname(__FILE__)}/ehriutils"

module Ehri

  EhriUtils::check_env

  # The magic necessary to do Java stuff...
  require "java"

  # Import Java classes like so...
  java_import "com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph"
  java_import "com.tinkerpop.frames.FramedGraph"
  java_import "eu.ehri.project.core.GraphManagerFactory"
  java_import "eu.ehri.project.models.EntityClass"
  java_import "eu.ehri.project.models.base.Frame"
  java_import "eu.ehri.project.definitions.EventTypes"
  java_import "com.google.common.base.Optional"

  # Define most stuff within constant packages
  module Core
    include_package "eu.ehri.project.core"
  end

  module Importers
    include_package "eu.ehri.project.importers"
  end

  module Persistance
    include_package "eu.ehri.project.persistance"
  end

  module Models
    include_package "eu.ehri.project.models"
  end

  module Acl
    include_package "eu.ehri.project.acl"
  end

  module Commands
    include_package "eu.ehri.project.commands"
  end

  module Views
    include_package "eu.ehri.project.views"
  end

  DB_PATH = ENV['NEO4J_DB'] ||= "#{ENV['NEO4J_HOME']}/data/graph.db"

  # Initialise a graph and the manager.
  # Note: the graph must not be being used elsewhere (i.e. by the server)
  Graph = FramedGraph.new(Neo4jGraph.new DB_PATH)
  Manager = GraphManagerFactory.get_instance Graph

end
