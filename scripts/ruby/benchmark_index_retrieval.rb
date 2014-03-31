#!/usr/bin/env jruby

# 
# Quick and dirty benchmarking script to try and compare
# bulk lookups for native graph IDs vs. index lookups.
#

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"
require 'benchmark'


module Ehri
  module Fixer
    include Ehri

    types = {
      EntityClass::DOCUMENTARY_UNIT => Models::DocumentaryUnit.java_class,
      EntityClass::REPOSITORY => Models::Repository.java_class,
      EntityClass::HISTORICAL_AGENT => Models::HistoricalAgent.java_class
    }
    rand_choose = ARGV.shift.to_i rescue 5

    10.times do 

      values = []

      types.each do |t, c|
        Manager.get_frames(t, c).each do |item|
          if rand(0..rand_choose) == rand_choose
            values << [item.as_vertex.get_id, item.get_id]
          end
        end
      end

      values.shuffle!

      num_items = values.count

      Benchmark.bmbm(7) do |bm|
        bm.report("gid #{num_items}") do
          values.each do |v|
            Graph.get_vertex(v[0])
          end
        end

        bm.report("uid #{num_items}") do
          values.each do |v|
            Manager.get_vertex(v[1])
          end
        end
      end
    end

    Graph.get_base_graph.shutdown
  end
end
