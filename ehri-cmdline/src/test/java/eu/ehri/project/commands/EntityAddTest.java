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

import eu.ehri.project.models.Country;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.test.AbstractFixtureTest;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;

import static eu.ehri.project.commands.CmdEntryPoint.RetCode.BAD_DATA;
import static eu.ehri.project.commands.CmdEntryPoint.RetCode.OK;
import static org.junit.Assert.assertEquals;


public class EntityAddTest extends AbstractFixtureTest {
    @Test
    public void testEntityAdd() throws Exception {
        String[] args = new String[]{EntityClass.COUNTRY.getName(),
                "-P", "name=Elbonia", "-P", "identifier=el",
                "--user", "mike"};
        EntityAdd ua = new EntityAdd();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(OK.getCode(), ua.execWithOptions(graph, cmdLine));
        Country el = manager.getFrame("el", EntityClass.COUNTRY, Country.class);
        assertEquals("Elbonia", el.getProperty("name"));
    }

    @Test
    public void testEntityAddWithBadType() throws Exception {
        String[] args = new String[]{EntityClass.ADDRESS.getName(),
                "-P", "name=Elbonia", "-P", "identifier=el",
                "--user", "mike"};
        EntityAdd ua = new EntityAdd();
        CommandLine cmdLine = ua.getCmdLine(args);
        assertEquals(BAD_DATA.getCode(), ua.execWithOptions(graph, cmdLine));
    }
}
