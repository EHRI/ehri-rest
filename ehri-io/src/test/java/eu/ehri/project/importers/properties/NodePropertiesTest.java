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

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.slf4j.LoggerFactory;


public class NodePropertiesTest {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NodePropertiesTest.class);
    
    NodeProperties pc;
    @Before
    public void init(){
        pc  = new NodeProperties();
        pc.setTitles("node,property,handlerTempName,required?,multivalued?");
        pc.addRow("description,identifier,descriptionIdentifier,1,");
        pc.addRow("authorityDescription,otherFormsOfName,,,1");
        pc.addRow("description,languageCode,,1,");
        pc.addRow("description,name,,1,");
    } 
    public NodePropertiesTest() {
    }

    @Test
    public void descriptionHasIdentifier() {
        assertTrue(pc.hasProperty("description", "identifier"));
        assertFalse(pc.hasProperty("description", "descriptionIdentifier"));
    }
    
    @Test
    public void descriptionMustHaveIdentifier() {
        assertTrue(pc.isRequiredProperty("description", "identifier"));
        assertFalse(pc.isRequiredProperty("description", "descriptionIdentifier"));
        assertFalse(pc.isRequiredProperty("authorityDescription", "otherFormsOfName"));
    }
    
    @Test
    public void otherFormsOfNameIsMultivalued(){
        assertTrue(pc.isMultivaluedProperty("authorityDescription", "otherFormsOfName"));
        assertFalse(pc.isMultivaluedProperty("authorityDescription", "something"));
    }
    
    @Test
    public void descriptionIdentifierIsHandlerName(){
        assertEquals("descriptionIdentifier", pc.getHandlerName("description", "identifier"));
    }
    
    @Test
    public void getAttributeWithValue(){
         String propfile = "bundesarchive.properties";
        logger.debug(propfile);
        XmlImportProperties f = new XmlImportProperties(propfile);
        
        assertTrue(f.containsProperty("scopecontent/"));
//        phystech/p/=physicalCharacteristics
        assertTrue(f.containsProperty("phystech/p/"));
        assertEquals("physicalCharacteristics", f.getProperty("phystech/p/"));

        //did/unitid/@ehrilabel$ehri_main_identifier=objectIdentifier

        assertTrue(f.containsProperty("did/unitid/@encoding$Bestandssignatur"));
        assertEquals("objectIdentifier", f.getProperty("did/unitid/@encoding$Bestandssignatur"));
    }
    
    @Test
    public void getPropertyWithoutSchema(){
        String propfile = "dceuropeana.properties";
        logger.debug(propfile);
        XmlImportProperties f = new XmlImportProperties(propfile);
        
        assertTrue(f.containsProperty("identifier/"));
        assertEquals("objectIdentifier", f.getProperty("identifier/"));
        
    }
}
