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

import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for GraphML dump. NB: Unlike the {@link eu.ehri.project.commands.GraphSON}
 * command loading the dump file will not result in an unchanged graph due to
 * lack of support for array properties.
 */
public class GraphMLTest extends AbstractFixtureTest {
    @Test
    public void testSaveDump() throws Exception {
        File temp = File.createTempFile("temp-file-name", ".xml");
        temp.deleteOnExit();
        assertEquals(0L, temp.length());

        String filePath = temp.getAbsolutePath();
        String[] outArgs = new String[]{filePath};

        GraphML export = new GraphML();
        CommandLine outCmdLine = export.getCmdLine(outArgs);
        assertEquals(0, export.execWithOptions(graph, outCmdLine));
        assertTrue(temp.length() > 0L);
    }
}
