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

import eu.ehri.project.importers.eac.EacHandler;
import eu.ehri.project.importers.eac.EacImporter;

/**
 * Import EAC-CPF files.
 */
public class EacImport extends ImportCommand {

    final static String NAME = "eac-import";

    public EacImport() {
        super(EacHandler.class, EacImporter.class);
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] <neo4j-graph-dir> -user <user-id> -repo <agent-id> <eac1.xml> <eac2.xml> ... <eacN.xml>";
    }

    @Override
    public String getHelp() {
        return "Import an EAC file into the graph database, using the specified " +
                "repository and user.";
    }
}
