#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "ruby", "lib")

require "eagimporter"

USER_ID = "mike"
DATA_DIR = "eag"

Ehri::EagImporter::import(DATA_DIR, USER_ID)

