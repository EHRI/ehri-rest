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
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.VirtualEadHandler;
import eu.ehri.project.importers.VirtualEadImporter;

/**
 * Import EAD from the command line as a virtual collection.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class EadAsVirtualCollectionImport extends ImportCommand {

    final static String NAME = "virtual-ead-import";

    public EadAsVirtualCollectionImport() {
        this(VirtualEadHandler.class, VirtualEadImporter.class);
    }

    /**
     * Generic EAD import command, designed for extending classes that use specific Handlers.
     *
     * @param handler  The Handler class to be used for import
     * @param importer The Importer class to be used. If null, IcaAtomEadImporter is used.
     */
    public EadAsVirtualCollectionImport(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer) {
        super(handler, importer);
    }

    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <importing-user-id> -scope <responsible-user-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        return "Import an EAD file into the graph database as a tree of VirtualUnits, using the specified"
                + sep + "responsible User and importing User.";
    }
}
