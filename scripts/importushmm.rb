#!/usr/bin/env jruby


$:.unshift File.join(File.dirname(__FILE__), "ruby", "lib")

require "ushmmimporter"

USER_ID = "mike"
DATA_DIR = "ead"
REPO_ID = "us-005578"

Ehri::UshmmImporter::import(DATA_DIR, REPO_ID, USER_ID)


