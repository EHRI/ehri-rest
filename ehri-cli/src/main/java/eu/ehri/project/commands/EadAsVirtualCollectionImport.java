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

import eu.ehri.project.importers.ead.VirtualEadHandler;
import eu.ehri.project.importers.ead.VirtualEadImporter;

/**
 * Import EAD from the command line as a virtual collection.
 */
public class EadAsVirtualCollectionImport extends ImportCommand {

    final static String NAME = "virtual-ead-import";

    public EadAsVirtualCollectionImport() {
        super(VirtualEadHandler.class, VirtualEadImporter.class);
    }

    @Override
    public String getUsage() {
        return NAME + " [OPTIONS] -user <importing-user-id> -scope <responsible-user-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
    }

    @Override
    public String getHelp() {
        return "Import an EAD file into the graph database as a tree of VirtualUnits, " +
                "using the specified responsible user and importing User.";
    }
}
