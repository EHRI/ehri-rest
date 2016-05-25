#
# Importer for Ukraine CSV data.
#

require "#{File.dirname(__FILE__)}/ehri"


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

        log = Java::ComGoogleCommonBase::Optional.of("Importing spreadsheet data for Ukrainian repositories")
        ctx = Persistence::ActionManager.new(Graph).new_event_context(user, EventTypes::ingest, log)
        log = Importers::ImportLog.new(log)

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
            repo = Manager.get_entity(repo_code, Models::Repository.java_class)
            @repolookup[repo_code] = repo

            importer = Java::EuEhriProjectImportersCsv::UkrainianUnitImporter.new(Graph, repo, user, log)
            @importerlookup[repo_code] = importer

            importer.add_callback do |mutation|
              case mutation.get_state
                when Persistence::MutationState::CREATED
                  puts "Created item: #{mutation.get_node.get_id}"
                  ctx.add_subjects mutation.get_node
                  log.add_created
                when Persistence::MutationState::UPDATED
                  puts "Updated item: #{mutation.get_node.get_id}"
                  ctx.add_subjects mutation.get_node
                  log.add_updated
                else log.add_unchanged
              end
            end
          end

          begin
            importer.import_item(data)
          rescue Exceptions::ValidationError => e
            # Ignore validation errors... 
            puts e.get_message
          end
        end

        if log.has_done_work
          ctx.commit()
        end

        log
      end

      def import
        begin

          # lookup user
          user = Manager.get_entity(@user_id, Models::UserProfile.java_class)
          log = import_csv(user)
          log.print_report
        rescue Java::JavaLang::Exception => e
          e.print_stack_trace
          raise
        end
      end
    end

    def self.import(csv_file, user_id)
      Importer.new(csv_file, user_id).import
    end
  end
end

