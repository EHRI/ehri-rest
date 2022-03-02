/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.commands;

import eu.ehri.project.importers.csv.PersonalitiesImporter;

/**
 * Specialisation of {@link ImportCsvCommand} for importing personalities
 * from CSV files.
 */
public class PersonalitiesImport extends ImportCsvCommand {

    final static String NAME = "csv-pers-import";

    public PersonalitiesImport() {
        super(PersonalitiesImporter.class);
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -scope <scope-id> <csv-file1> " +
                "<csv-file2> ... <csv-fileN>";
    }

    @Override
    public String getHelp() {
        return "Import a CSV file as Personalities into the graph database, " +
                "using the specified scope and user.";
    }
}
