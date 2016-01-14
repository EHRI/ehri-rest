#!/usr/bin/env jruby

# Import country report data from a directory with all
# the files named ala "{countrycode}_{dbid}.md".
#

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

DIR = ARGV.shift

module Ehri
  module Fixer
    include Ehri

    module ModelsBase
      include_package "eu.ehri.project.models.base"
    end

    done = {}

    # Set a _childCount property on system events for how many subjects they have
    total = 0
    begin
      Manager.get_entities(EntityClass::COUNTRY, Models::Country.java_class).each do |ct|
        id = ct.get_id
        files = Dir.glob("#{DIR}/#{id}_???.md")
        if not files.empty?
          file = files[0]
          puts "#{id} -> #{file}"
          total += 1
          done[id] = true
          content = File.open(file, "r:UTF-8") { |f| f.read }
          ct.as_vertex.set_property("report", content)
        end
      end

      allfiles = Dir.glob("#{DIR}/*.md")
      allfiles.each do |f|
        bn = File.basename(f)
        cc = $1 if bn =~ /^([a-z]{2})_.+md$/
        if not done.has_key?(cc)
          content = File.open(f, "r:UTF-8") { |fh| fh.read }
          bundle = Persistence::Bundle.new(EntityClass::COUNTRY)
            .with_data_value("identifier", cc)
            .with_data_value("report", content)
          persister = Persistence::BundleDAO.new(Graph)
          persister.create(bundle, Models::Country.java_class)
        end
      end

      Graph.get_base_graph.commit
      puts "Committed: #{total}"
      Graph.get_base_graph.shutdown
    rescue Exception => msg
      puts "ERROR - #{msg}"
    end
  end
end
