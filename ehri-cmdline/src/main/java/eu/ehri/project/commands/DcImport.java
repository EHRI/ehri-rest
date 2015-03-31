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


import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.DcEuropeanaHandler;
import eu.ehri.project.importers.EadImporter;
import eu.ehri.project.importers.SaxXmlHandler;

/**
 * Import EAD from the command line...
 */
public class DcImport extends ImportCommand implements Command {

    final static String NAME = "dc-import";

    public DcImport() {
        this(DcEuropeanaHandler.class, EadImporter.class);
    }

    /**
     * Generic EAD import command, designed for extending classes that use specific Handlers.
     *
     * @param handler  The Handler class to be used for import
     * @param importer The Importer class to be used. If null, IcaAtomEadImporter is used.
     */
    public DcImport(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer) {
        super(handler, importer);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <user-id> -scope <repository-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        return "Import an DC file into the graph database, using the specified"
                + sep + "Repository and User.";
    }
}
