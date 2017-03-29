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

package eu.ehri.project.importers.ead;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VirtualEadTest extends AbstractImporterTest {
    private static final String REPO1 = "002777";
    private static final String REPO2 = "002302";
    private static final String UNIT1 = "wp2_bt";
    private static final String UNIT2 = "vzpomínky pro EHRI";

    private static final String ARCHDESC = "ehri terezin research guide";
    private static final String C01_VirtualLevel = "vc_tm";

    Repository repository1, repository2;
    DocumentaryUnit unit1, unit2;

    @Test
    public void setStageTest() throws Exception {
        setStage();
        assertEquals(REPO1, repository1.getIdentifier());
        assertEquals(UNIT1, unit1.getIdentifier());
    }

    @Test
    public void virtualUnitTest() throws Exception {

        setStage();

        PermissionScope user = manager.getEntity("mike", PermissionScope.class);
        final String logMessage = "Importing an EAD as a Virtual collection";

        int origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream("wp2_virtualcollection.xml");

        saxImportManager(VirtualEadImporter.class, VirtualEadHandler.class, "vc.properties")
                .withScope(user)
                .importInputStream(ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        // How many new nodes will have been created? We should have
        // - 2 more VirtualUnits (archdesc, 1 child (other 2 children are already existing DUs))
        // - 2 more DocumentDescription
        // - 3 more import Event links (2 for every Unit, 1 for the User)
        // - 2 more maintenance events
        // - 1 more import Event
        int newCount = origCount + 10;
        assertEquals(newCount, getNodeCount(graph));

        VirtualUnit toplevel = graph.frame(getVertexByIdentifier(graph, ARCHDESC), VirtualUnit.class);
        assertEquals(user, toplevel.getAuthor());
        assertEquals("ehri terezin research guide", toplevel.getIdentifier());
        assertEquals(1, toList(toplevel.getIncludedUnits()).size());

        DocumentaryUnit c1_vreferrer = manager.getEntity(UNIT1, DocumentaryUnit.class);
        for (AbstractUnit d : toplevel.getIncludedUnits()) {
            assertEquals(c1_vreferrer, d);
        }

        VirtualUnit c1_vlevel = graph.frame(getVertexById(graph, toplevel.getId() + "-" + C01_VirtualLevel), VirtualUnit.class);
        assertEquals(toplevel, c1_vlevel.getParent());
        Iterable<DocumentaryUnitDescription> descriptions = c1_vlevel.getVirtualDescriptions();
        assertTrue(descriptions.iterator().hasNext());
        for (DocumentaryUnitDescription d : descriptions) {
            //the describedEntity of a VirtualLevel type VirtualUnit is the VirtualUnit itself
            assertEquals(c1_vlevel, d.getDescribedEntity());
        }

        int countIncludedUnits = 0;
        for (AbstractUnit included : c1_vlevel.getIncludedUnits()) {
            countIncludedUnits++;
            for (Description d : included.getDescriptions()) {
                assertEquals(UNIT2 + "title", d.getName());
                assertEquals("cze", d.getLanguageOfDescription());
            }
        }
        assertEquals(1, countIncludedUnits);
    }

    private void setStage() throws Exception {
        Bundle repo1Bundle = Bundle.of(EntityClass.REPOSITORY)
                .withDataValue(Ontology.IDENTIFIER_KEY, REPO1);
        Bundle repo2Bundle = Bundle.of(EntityClass.REPOSITORY)
                .withDataValue(Ontology.IDENTIFIER_KEY, REPO2);
        Bundle documentaryUnit1Bundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT)
                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT1);
        Bundle documentDescription1Bundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT1 + "desc")
                .withDataValue(Ontology.NAME_KEY, UNIT1 + "title")
                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "eng");

        Bundle documentaryUnit2Bundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT)
                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT2);
        Bundle documentDescription2Bundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT2 + "desc")
                .withDataValue(Ontology.NAME_KEY, UNIT2 + "title")
                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "cze");

        repository1 = api(validUser).create(repo1Bundle, Repository.class);
        repository2 = api(validUser).create(repo2Bundle, Repository.class);

        documentaryUnit1Bundle = documentaryUnit1Bundle.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, documentDescription1Bundle);
        unit1 = api(validUser).create(documentaryUnit1Bundle, DocumentaryUnit.class);
        unit1.setRepository(repository1);

        documentaryUnit2Bundle = documentaryUnit2Bundle.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, documentDescription2Bundle);
        unit2 = api(validUser).create(documentaryUnit2Bundle, DocumentaryUnit.class);
        unit2.setRepository(repository2);
    }
}
