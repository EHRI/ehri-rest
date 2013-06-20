#!/usr/bin/env ruby

require "#{File.dirname(__FILE__)}/ehri"

LANG = "en"

module Ehri
    module EagImporter

        include Ehri

        class Importer
            def initialize(data_dir, user_id)
                @data_dir = data_dir
                @user_id = user_id
            end

            def get_or_create_country(code, name, user)
                if Manager.exists(code)
                    Manager.get_frame(code, Models::Country.java_class)
                else
                    bundle = Persistance::Bundle.new(EntityClass::COUNTRY)
                                .with_data_value("identifier", code)
                                .with_data_value("name", name)
                    log = Optional::of("Creating country record for #{name}")
                    Views::ViewFactory.get_crud_with_logging(Graph, 
                                        Models::Country.java_class).create(bundle, user, log)
                end
            end

            def import_country(dir, user)

                countrycode = File.basename(dir)
                countryname = Java::JavaUtil::Locale.new(LANG, countrycode).getDisplayCountry
                repos = Dir.glob("#{dir}/????.xml")

                country = get_or_create_country(countrycode, countryname, user)

                msg = "Importing EAG for country #{countryname}"

                manager = Importers::SaxImportManager.new(Graph, country, user, 
                                               Importers::EagImporter.java_class,
                                               Importers::EagHandler.java_class)
                log = manager.import_files(repos, msg)

                puts "Done EAG import for #{countryname}: created: #{log.get_created}, updated: #{log.get_updated}"
            end

            def import
                begin
                    # lookup USHMM
                    user = Manager.get_frame(@user_id, Models::UserProfile.java_class)

                    # We basically need recursive behaviour here
                    Dir.glob("#{@data_dir}/??").sort.each do |dir|
                        import_country(dir, user)
                    end

                    Graph.get_base_graph.commit
                    puts "Commited"
                rescue
                    # Oops!
                    Graph.get_base_graph.rollback
                    raise
                end
            end
         end

        # Entry point...
        def self.import(data_dir, user_id)
            Importer.new(data_dir, user_id).import
        end
    end
end


