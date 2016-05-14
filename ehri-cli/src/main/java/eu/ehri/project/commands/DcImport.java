/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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


import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.xml.DcEuropeanaHandler;

/**
 * Import command for importing Dublin Core files.
 */
public class DcImport extends ImportCommand {

    final static String NAME = "dc-import";

    public DcImport() {
        super(DcEuropeanaHandler.class, EadImporter.class);
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] -user <user-id> -scope <repository-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getHelp() {
        return "Import an DC file into the graph database, using the specified " +
                "repository and user.";
    }
}
