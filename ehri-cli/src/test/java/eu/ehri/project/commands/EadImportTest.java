/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class EadImportTest extends AbstractFixtureTest {
    @Test
    public void testEadImport() throws Exception {
        String eadFile = getFixtureFilePath("single-ead.xml");
        String[] args = new String[]{"--user", "mike", "--scope", "r1", eadFile};

        EadImport ua = new EadImport();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(0, ua.execWithOptions(graph, cmdLine));
    }
}
