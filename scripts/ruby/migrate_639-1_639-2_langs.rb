#!/usr/bin/env jruby

# Migrate languageCode fields from IS639-1 (2-letter) to ISO639-2 (3-letter) codes.

$:.unshift File.join(File.dirname(__FILE__), "lib")
require "#{File.join(File.dirname(__FILE__), "lib", "ehri")}"

module Ehri
  module Fixer
    include Ehri

    @fields_to_check = ["languageCode", "languageOfMaterial"]
    @total = 0
    @errors = 0

    @lookup2to3 = Hash.new
    Java::JavaUtil::Locale.getISOLanguages.each do |lang|
      locale = Java::JavaUtil::Locale.new(lang)
      @lookup2to3[lang] = locale.getISO3Language
    end

    # Additional hacks for stuff we can infer is using countries, not lang codes
    @lookup2to3["cz"] = @lookup2to3["cs"]
    @lookup2to3["gr"] = @lookup2to3["el"]
    

    def self.get_3_code(code)
        if code.length == 2
          @lookup2to3.fetch(code)
        else
          code
        end
    end

    def self.get2to3(lang)
      if lang.class == Java::JavaUtil::ArrayList
        lang.collect { |l| get_3_code(l) }
      elsif lang.class == String
        get_3_code(lang)
      else
        raise "Unknown type for lang: #{lang.class}"
      end

    end

    # Doing a daft full-graph scan here...
    Graph.get_vertices.each do |v|
      begin
        @fields_to_check.each do |field|
          value = v.get_property(field)
          if not value.nil?
            v.set_property(field, get2to3(value))
            @total += 1
          end
        end
      rescue Exception => msg
        puts "ERROR - #{msg}"
        @errors += 1
      end
    end

    Core::GraphReindexer.new(Graph).reindex("entities")

    Graph.get_base_graph.commit
    puts "Committed: #{@total} , errors: #{@errors}"    
    Graph.get_base_graph.shutdown

  end
end
