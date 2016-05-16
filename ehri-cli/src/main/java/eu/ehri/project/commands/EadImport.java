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

import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;

/**
 * Import EAD from the command line.
 */
public class EadImport extends ImportCommand {

    final static String NAME = "ead-import";

    public EadImport() {
        this(EadHandler.class, EadImporter.class);
    }

    /**
     * Generic EAD import command, designed for extending classes that use specific Handlers.
     *
     * @param handler  The Handler class to be used for import
     * @param importer The Importer class to be used. If null, IcaAtomEadImporter is used.
     */
    public EadImport(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer) {
        super(handler, importer);
    }

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] -user <user-id> " +
                "-scope <repository-id> <ead1.xml> <ead2.xml> ... <eadN.xml>", NAME);
    }

    @Override
    public String getHelp() {
        return "Import an EAD file into the graph database, using the specified " +
                "repository and user.";
    }
}
