#
# Import ICA-Atom repository exports in EAG XML. The
# individual EAG XML files reside in a directory named
# for the country code (IS03166 - 2-letter) to which they
# belong.
#

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
          Manager.get_entity(code, Models::Country.java_class)
        else
          bundle = Persistence::Bundle.new(EntityClass::COUNTRY)
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

        manager = Managers::SaxImportManager.new(Graph, country, user, 
                                                  Importers::EagImporter.java_class,
                                                  Importers::EagHandler.java_class)
        log = manager.import_files(repos, msg)

        log.print_report
        log.get_created + log.get_updated
      end

      def import
        # lookup USHMM
        user = Manager.get_entity(@user_id, Models::UserProfile.java_class)

        changed = 0
        # We basically need recursive behaviour here
        Dir.glob("#{@data_dir}/??").sort.each do |dir|
          changed += import_country(dir, user)
        end

        if changed > 0
          puts "Changed #{changed}, Committed"
        else
          puts "No changes"
        end
      end
    end

    # Entry point...
    def self.import(data_dir, user_id)
      Importer.new(data_dir, user_id).import
    end
  end
end


