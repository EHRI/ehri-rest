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
import org.junit.Test;

import static org.junit.Assert.assertTrue;


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
        pc.addRow("Unit,identifier,objectIdentifier,1,");
        pc.addRow("Description,identifier,descriptionIdentifier,1,");
        pc.addRow("Description,languageCode,,1,");
        pc.addRow("Description,name,,1,");
        pc.addRow("RepositoryDescription,typeOfEntity,,,");
        pc.addRow("RepositoryDescription,otherFormsOfName,,,1");
        pc.addRow("RepositoryDescription,parallelFormsOfName,,,");
        pc.addRow("RepositoryDescription,history,,,");
        pc.addRow("RepositoryDescription,generalContext,,,");
        p = new PropertiesChecker(pc);
    }

    public PropertiesCheckerTest() {
    }

    @Test
    public void testCheck() {
        assertTrue(p.check(new XmlImportProperties("eag.properties"), EntityClass.REPOSITORY_DESCRIPTION));
    }
}
