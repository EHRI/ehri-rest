/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.properties;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
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

        assertTrue(f.containsProperty("did/unitid/@ehrilabel$ehri_main_identifier"));
        assertEquals("objectIdentifier", f.getProperty("did/unitid/@ehrilabel$ehri_main_identifier"));
        assertEquals("ehri_main_identifier", f.getValueForAttributeProperty("did/unitid/", "@ehrilabel"));
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
