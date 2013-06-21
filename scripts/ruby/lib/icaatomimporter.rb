#
# Importer for an ICA-Atom EAD export. Individual EAD XML
# files reside in a directory named with their repository
# id. The EAD files are potentially hierarchical, so there
# is no need for a hierarchical directory structure.
#

require "#{File.dirname(__FILE__)}/ehri"

module Ehri
  module IcaAtomImporter

    include Ehri

    class Importer
      def initialize(data_dir, user_id)
        @data_dir = data_dir
        @user_id = user_id
      end

      def import_repo_data(dir, user)

        repo_id = File.basename(dir)
        repo = Manager.get_frame(repo_id, Models::Repository.java_class)
        ead_files = Dir.glob("#{dir}/*.xml")

        msg = "Importing ICA-Atom EAD for repository #{repo_id}"

        manager = Importers::SaxImportManager.new(
          Graph, repo, user,
          Importers::IcaAtomEadImporter.java_class,
          Importers::IcaAtomEadHandler.java_class)
        log = manager.import_files(ead_files, msg)

        puts "Done EAD import for #{repo_id}: created: #{log.get_created}, updated: #{log.get_updated}"
      end

      def import
        begin

          # lookup USHMM
          user = Manager.get_frame(@user_id, Models::UserProfile.java_class)

          # We basically need recursive behaviour here
          Dir.glob("#{@data_dir}/??-??????").sort.each do |dir|
            import_repo_data(dir, user)
          end

          Graph.get_base_graph.commit
          puts "Committed"
        rescue
          # Oops!
          Graph.get_base_graph.rollback
          raise
        end
      end
    end

    def self.import(data_dir, user_id)
      Importer.new(data_dir, user_id).import
    end
  end
end

