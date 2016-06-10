#
# Importer for a USHMM Solr-to-EAD dump.
#
# The directory structure is a bit ad-hoc at the moment,
# with EAD XML files residing in a directory named for
# their immediate parent (or at the top level.)
#
# This is basically a hack.
#

require "#{File.dirname(__FILE__)}/ehri"

# Number of files to import before committing the tx
COMMIT_MAX = 2500


module Ehri
  module UshmmImporter

    include Ehri

    class Importer
      def initialize(data_dir, repo_id, user_id)
        @data_dir = data_dir
        @user_id = user_id
        @repo_id = repo_id
      end

      def import_with_scope(xmlpath, scope, event, user, log)
        puts "Importing #{xmlpath}"
        child_path = File.join @data_dir, xmlpath.match(/irn(?<num>\d+)\.xml$/)["num"]

        children = []
        if Dir.exist? child_path
          children = Dir.glob("#{child_path}/irn*xml")
        end

        importer = Java::EuEhriProjectImportersEad::IcaAtomEadImporter.new(Graph, scope, user, log)

        importer.add_callback do |mutation|
          case mutation.get_state
            when Persistence::MutationState::CREATED
              puts "Created item: #{mutation.get_node.get_id}"
              event.add_subjects mutation.get_node
              log.add_created
            when Persistence::MutationState::UPDATED
              puts "Updated item: #{mutation.get_node.get_id}"
              event.add_subjects mutation.get_node
              log.add_updated
            else log.add_unchanged
          end

          if log.get_changed > 0 and log.get_changed % COMMIT_MAX == 0
            Graph.get_base_graph.commit
          end
          children.each do |cxml|
            import_with_scope(cxml, mutation.get_node, event, user, log)
          end
        end

        handler = Java::EuEhriProjectImportersEad::UshmmHandler.new importer
        spf = Java::JavaxXmlParsers::SAXParserFactory.new_instance
        spf.set_namespace_aware false
        spf.set_validating false
        spf.set_schema nil
        parser = spf.new_sax_parser

        File.open(xmlpath, "r") do |f|
          parser.parse(f.to_inputstream, handler)
        end
      end


      def import
        begin
          # lookup USHMM
          ushmm = Manager.get_entity(@repo_id, Models::Repository.java_class)
          user = Manager.get_entity(@user_id, Models::UserProfile.java_class)

          # Start an action!
          msg = Java::ComGoogleCommonBase::Optional.of("Importing USHMM data")
          ctx = Persistence::ActionManager.new(
            Graph, ushmm).new_event_context(user, EventTypes::ingest, msg)
          log = Importers::ImportLog.new(msg)

          # We basically need recursive behaviour here
          Dir.glob("#{@data_dir}/irn*xml").each do |xmlpath|
            import_with_scope xmlpath, ushmm, ctx, user, log
          end

          log.print_report

          if log.has_done_work
            ctx.commit
            puts "Committed"
          end
        rescue
          # Oops!
          raise
        end
      end
    end

    def self.import(data_dir, repo_id, user_id)
      Importer.new(data_dir, repo_id, user_id).import
    end
  end
end

