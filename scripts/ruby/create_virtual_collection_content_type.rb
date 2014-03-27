#!/usr/bin/env jruby

# Create a content type for virtual collections

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

USER = ARGV.shift


module Ehri
  module Fixer
    include Ehri
    Manager.create_vertex(
        Acl::ContentTypes::VIRTUAL_COLLECTION.get_name,
        Models::EntityClass::CONTENT_TYPE,
        {}
    )
    Graph.get_base_graph.commit

    Core::GraphReindexer.new(Graph).reindex(Core::GraphReindexer::INDEX_NAME)
    puts "Committed and re-indexed"
    Graph.get_base_graph.shutdown
  end
end
