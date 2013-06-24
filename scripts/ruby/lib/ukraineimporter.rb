#
# Importer for an ICA-Atom EAD export. Individual EAD XML
# files reside in a directory named with their repository
# id. The EAD files are potentially hierarchical, so there
# is no need for a hierarchical directory structure.
#

require "#{File.dirname(__FILE__)}/ehri"

require "csv"


module Ehri
  module UkraineImporter

    include Ehri

    java_import "au.com.bytecode.opencsv.CSVReader"

    class Importer


      def initialize(csv_file, user_id)
        @csv_file = csv_file
        @user_id = user_id
        @repolookup = {}
        @importerlookup = {}
      end

      def import_csv(user)
        
        # Note gotcah on char!
        csv = CSVReader.new(Java::JavaIo::FileReader.new(@csv_file), ";"[0].ord.to_java(:char))

        headers = csv.read_next().to_a

        while (row = csv.read_next) != nil
          data = Hash[*headers.zip(row.to_a).flatten]
          repo_code = "ua-%06d" % data["repository_code"]
          repo = @repolookup[repo_code]
          importer = @importerlookup[repo_code]

          # if we don't already have the repo/importer, look them up
          # and 
          if repo.nil?
            
            # Let this throw a fatal error if the repo can't be found - they
            # should ensure all the data refers to valid repositories.
            repo = Manager.get_frame(repo_code, Models::Repository.java_class)

            @repolookup[repo_code] = repo
            puts "Got repo: #{repo.get_id}"

            ctx = Persistance::ActionManager.new(
              Graph, repo).log_event(user, EventTypes::ingest, "Importing spreadsheet data for #{repo_code}")
            log = Importers::ImportLog.new(ctx)
            importer = Importers::UkrainianUnitImporter.new(Graph, repo, log)
            @importerlookup[repo_code] = importer

            importer.add_creation_callback do |item|
              puts "Created item: #{item.get_id}"
              ctx.add_subjects item
              log.add_created
            end

            importer.add_update_callback do |item|
              puts "Updated item: #{item.get_id}"
              ctx.add_subjects item
              log.add_updated
            end
          end

          begin
            importer.import_item(data)
          rescue Exceptions::ValidationError => e
            # Ignore validation errors... 
            puts e.get_message
          end
        end
      end

      def import
        begin

          # lookup user
          user = Manager.get_frame(@user_id, Models::UserProfile.java_class)
          import_csv(user)

          Graph.get_base_graph.commit
          puts "Committed"
        rescue
          # Oops!
          Graph.get_base_graph.rollback
          raise
        end
      end
    end

    def self.import(csv_file, user_id)
      Importer.new(csv_file, user_id).import
    end
  end
end

