/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.properties;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author linda
 */
public class NodePropertiesTest {
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
}
