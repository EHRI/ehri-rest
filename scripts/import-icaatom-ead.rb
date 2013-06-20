#!/usr/bin/env jruby

$:.unshift File.join(File.dirname(__FILE__), "ruby", "lib")

require "icaatomimporter"

USER_ID = "mike"
DATA_DIR = "icaatom-export"

Ehri::IcaAtomImporter::import(DATA_DIR, USER_ID)
