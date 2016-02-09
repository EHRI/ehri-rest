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

import eu.ehri.project.importers.eag.EagHandler;
import eu.ehri.project.importers.eag.EagImporter;

/**
 * Import EAG files.
 */
public class EagImport  extends ImportCommand {

    final static String NAME = "eag-import";

    public EagImport() {
        super(EagHandler.class, EagImporter.class);
    }
    
    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] -user <user-id> -scope <agent-id> <neo4j-graph-dir> <eag1.xml> <eag2.xml> ... <eagN.xml>";
    }

    @Override
    public String getHelp() {
        return "Import an EAG file into the graph database, using the specified repository and user.";
    }
}
