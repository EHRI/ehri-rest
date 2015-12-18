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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class EntityDeleteTest extends AbstractFixtureTest {
    @Test(expected = ItemNotFound.class)
    public void testExecWithOptions() throws Exception {
        String[] args = new String[]{"reto", "--user", "mike", "--log", "Goodbye, Reto"};

        EntityDelete del = new EntityDelete();
        CommandLine cmdLine = del.getCmdLine(args);
        assertEquals(0, del.execWithOptions(graph, cmdLine));
        manager.getFrame("reto", UserProfile.class);
    }
}
