#!/usr/bin/env jruby

$:.unshift File.join(File.dirname(__FILE__), "ruby", "lib")

require "ukraineimporter"

USER_ID = "mike"
CSV_FILE =  "#{ENV["HOME"]}/Dropbox/EHRI-WP19-20/TestData/ukraine.csv"

Ehri::UkraineImporter::import(CSV_FILE, USER_ID)

