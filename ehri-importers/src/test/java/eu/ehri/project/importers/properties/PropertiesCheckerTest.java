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
public class PropertiesCheckerTest {

    PropertiesChecker p;

    @Before
    public void init() {
        NodeProperties pc = new NodeProperties();
        pc.setTitles(NodeProperties.NODE + NodeProperties.SEP + 
                NodeProperties.PROPERTY + NodeProperties.SEP + 
                NodeProperties.HANDLERNAME + NodeProperties.SEP + 
                NodeProperties.REQUIRED + NodeProperties.SEP + 
                NodeProperties.MULTIVALUED);
        pc.addRow("unit,identifier,objectIdentifier,1,");
        pc.addRow("description,identifier,descriptionIdentifier,1,");
        pc.addRow("description,languageCode,,1,");
        pc.addRow("description,name,,1,");
        pc.addRow("repositoryDescription,typeOfEntity,,,");
        pc.addRow("repositoryDescription,otherFormsOfName,,,1");
        pc.addRow("repositoryDescription,parallelFormsOfName,,,");
        pc.addRow("repositoryDescription,history,,,");
        pc.addRow("repositoryDescription,generalContext,,,");
        p = new PropertiesChecker(pc);
    }

    public PropertiesCheckerTest() {
    }

    @Test
    public void testCheck() {
        assertTrue(p.check(new XmlImportProperties("eag.properties"), "repositoryDescription"));
    }
}
