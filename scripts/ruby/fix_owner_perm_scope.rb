#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

# Owner permissions were incorrectly granted with a scope,
# which meant Things Went Wrong (when fetching permissions
# for display, not when calculating perms for update.)
#
# Check ALL permission grants with a scope have a content-type
# target - not an item target.
#

module Ehri
  module Fixer
    include Ehri

    total = 0
    Manager.get_frames(EntityClass::PERMISSION_GRANT, Models::PermissionGrant.java_class).each do |pg|
      begin
        scope = pg.get_scope
        targets = pg.get_targets.iterator.to_a
        if not scope.nil?
          puts "#{scope.get_id} -> #{targets.collect{|t| t.get_id}}"
          bad = false
          targets.each do |t|
            if not t.get_type == EntityClass::CONTENT_TYPE.to_s
              puts " - BAD: #{t.get_id} #{t.get_type}"
              bad = true
              break
            end
          end
          if bad
            pg.remove_scope(scope)
            total += 1
          end
        end
      rescue Exception => msg
        puts "ERROR - #{msg}"
      end
    end
    if total > 0
      Graph.get_base_graph.commit
      puts "Fixed: #{total}"
    end
    Graph.get_base_graph.shutdown
  end
end
