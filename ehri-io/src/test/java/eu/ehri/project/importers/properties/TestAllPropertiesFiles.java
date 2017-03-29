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

package eu.ehri.project.importers.properties;

import eu.ehri.project.models.EntityClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestAllPropertiesFiles {

    private static final Logger logger = LoggerFactory.getLogger(TestAllPropertiesFiles.class);
    PropertiesChecker p;

    @Before
    public void init() {
        NodeProperties pc = new NodeProperties();
        try {
            InputStream fis;
            BufferedReader br;

            fis = ClassLoader.getSystemClassLoader().getResourceAsStream("allowedNodeProperties.csv");
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            String firstline = br.readLine();
            pc.setTitles(firstline);

            String line;
            while ((line = br.readLine()) != null) {
                pc.addRow(line);
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            fail();
        }
        p = new PropertiesChecker(pc);
    }

    @Test
    public void testEacXmlProperties() {
        assertTrue(p.check(new XmlImportProperties("eac.properties"), EntityClass.HISTORICAL_AGENT_DESCRIPTION));
    }

    @Test
    public void testPersonalitiesProperties() {
        assertTrue(p.check(new XmlImportProperties("personalities.properties"), EntityClass.HISTORICAL_AGENT_DESCRIPTION));
    }

    @Test
    public void testEagXmlProperties() {
        assertTrue(p.check(new XmlImportProperties("eag.properties"), EntityClass.REPOSITORY_DESCRIPTION));
    }

    @Test
    public void testDansEadProperties() {
        String propfile = "dansead.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    @Ignore //not used anymore
    public void testEadXmlProperties() {
        String propfile = "ead2002.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testWp2EadXmlProperties() {
        String propfile = "wp2ead.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testSkosXmlProperties() {
        assertTrue(p.check(new XmlImportProperties("skos.properties"), EntityClass.CVOC_CONCEPT_DESCRIPTION));
    }

    @Test
    @Ignore //ignore in favour of niodead.properties
    public void testNiodXmlProperties() {
        String propfile = "niod.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testNiodEadXmlProperties() {
        String propfile = "niodead.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
        XmlImportProperties prop = new XmlImportProperties(propfile);
        assertEquals("xref", prop.getAttributeProperty("href"));
        assertThat(prop.getPropertiesWithValue("findingAids"), hasItem("otherfindaid/p/"));
    }

    @Test
    public void testUshmmXmlProperties() {
        String propfile = "ushmm.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testDcEuropeanaProperties() {
        String propfile = "dceuropeana.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testBundesarchiveProperties() {
        String propfile = "bundesarchive.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testItsFindingaidsProperties() {
        String propfile = "its-provenance.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testItsHoldingguidesProperties() {
        String propfile = "its-pertinence.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testCegesomaAAProperties() {
        String propfile = "cegesomaAA.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testCegesomaABProperties() {
        String propfile = "cegesomaAB.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testCegesomaCAProperties() {
        String propfile = "cegesomaCA.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }

    @Test
    public void testGenericProperties() {
        String propfile = "generic.properties";
        logger.debug(propfile);
        assertTrue(p.check(new XmlImportProperties(propfile), EntityClass.DOCUMENTARY_UNIT_DESCRIPTION));
    }
}
