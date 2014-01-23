#!/usr/bin/env jruby

# Create the additional 'promote' permission

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

USER = ARGV.shift


module Ehri
  module Fixer
    include Ehri

    Manager.create_vertex(
        Acl::PermissionType::PROMOTE.get_name,
        Models::EntityClass::PERMISSION,
        {}
    )
    Graph.get_base_graph.commit
    puts "Committed!"
    Graph.get_base_graph.shutdown
  end
end
