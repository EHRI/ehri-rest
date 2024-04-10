/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.PermissionScope;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;


public class IpnTest extends AbstractImporterTest {

    protected final String BRANCH_1_XMLFILE = "polishBranch.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String BRANCH_1_ARCHDESC = "Pamięci Narodowej",
            BRANCH_1_C01_1 = "2746",
            BRANCH_1_C01_2 = "2747";

    protected final String BRANCH_2_XMLFILE = "polishBranch_2.xml";
    // Identifiers of nodes in the imported documentary units
    protected final String BRANCH_2_ARCHDESC = "Biuro Udostępniania",
            BRANCH_2_C01_1 = "1",
            BRANCH_2_C01_2 = "2";

    protected final String VC_XMLFILE = "IpnVirtualCollection.xml";

    @Test
    @Ignore
    public void polishVirtualCollectionTest() throws Exception {
        final String logMessage = "Importing a part of the IPN Virtual Collection";

        try (InputStream ios1 = ClassLoader.getSystemResourceAsStream(BRANCH_1_XMLFILE)) {
            saxImportManager(EadImporter.class, EadHandler.class, "polishBranch.properties")
                    .importInputStream(ios1, logMessage);
        }

        try (InputStream ios2 = ClassLoader.getSystemResourceAsStream(BRANCH_2_XMLFILE)) {
            saxImportManager(EadImporter.class, EadHandler.class, "polishBranch.properties")
                    .importInputStream(ios2, logMessage);
        }

        int origCount = getNodeCount(graph);
        try (InputStream iosVc = ClassLoader.getSystemResourceAsStream(VC_XMLFILE)) {
            saxImportManager(VirtualEadImporter.class, VirtualEadHandler.class, "vc.properties")
                    .importInputStream(iosVc, logMessage);
        }

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 3 more VirtualUnits (archdesc, 2 children with each 2 children)
        // - 3 more DocumentDescription
        // - 4 more import Event links (6 for every Unit, 1 for the User)
        // - 1 more import Event

        // - 0 more MaintenanceEvents
        int newCount = origCount + 11;
        assertEquals(newCount, getNodeCount(graph));

        VirtualUnit archdesc = graph.frame(
                getVertexByIdentifier(graph, "ipn vc"),
                VirtualUnit.class);

        assertEquals(2, archdesc.countChildren());
    }

    @Test
    public void polishBranch_1_EadTest() throws Exception {

        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a the IPN Polish Branches EAD, without preprocessing done";

        int origCount = getNodeCount(graph);

        // Before...
        try (InputStream ios = ClassLoader.getSystemResourceAsStream(BRANCH_1_XMLFILE)) {
            saxImportManager(EadImporter.class, EadHandler.class, "polishBranch.properties")
                    .importInputStream(ios, logMessage);
        }
        // After...

        // How many new nodes will have been created? We should have
        /*
         * null: 4
         * relationship: 4
         * documentaryUnit: 3
         * property: 1
         * documentDescription: 3
         * maintenanceEvent: 3
         * systemEvent: 1
         * datePeriod: 2
         */

        int newCount = origCount + 21;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph, BRANCH_1_ARCHDESC),
                DocumentaryUnit.class);
        DocumentaryUnit c1_1 = graph.frame(
                getVertexByIdentifier(graph, BRANCH_1_C01_1),
                DocumentaryUnit.class);
        DocumentaryUnit c1_2 = graph.frame(
                getVertexByIdentifier(graph, BRANCH_1_C01_2),
                DocumentaryUnit.class);

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1_1.getParent());
        assertEquals(archdesc, c1_1.getPermissionScope());
        assertEquals(archdesc, c1_2.getParent());
        assertEquals(archdesc, c1_2.getPermissionScope());


        //test titles
        for (DocumentaryUnitDescription d : archdesc.getDocumentDescriptions()) {
            assertEquals("Collections from Oddział Instytutu Pamięci Narodowej we Wrocławiu", d.getName());
            boolean hasProvenance = false;
            for (String property : d.getPropertyKeys()) {
                if (property.equals("processInfo")) {
                    hasProvenance = true;
                    assertTrue(d.<List<String>>getProperty(property)
                            .get(0).startsWith("This selection has been "));
                }
            }
            assertTrue(hasProvenance);
        }
        for (DocumentaryUnitDescription desc : c1_1.getDocumentDescriptions()) {
            for (String p : desc.getPropertyKeys()) {
                System.out.println(p + " --> " + desc.getProperty(p));
            }
            assertEquals("Cukrownia w Pszennie – August Gross i Synowie [August Gross & Söhne Zuckerfabrik Weizenrodau]", desc.getName());
            assertFalse(desc.getPropertyKeys().contains("unitDates"));
        }
        //test hierarchy
        assertEquals(2, archdesc.countChildren());

        //test level-of-desc
        for (DocumentaryUnitDescription d : c1_1.getDocumentDescriptions()) {
            assertEquals("collection", d.getProperty("levelOfDescription"));
        }
        // test dates
        boolean hasDates = false;
        for (DocumentaryUnitDescription d : c1_1.getDocumentDescriptions()) {
            hasDates = d.getDatePeriods().iterator().hasNext();
        }
        assertTrue(hasDates);
    }

    @Test
    @Ignore
    public void polishBranch_2_EadTest() throws Exception {

        PermissionScope agent = manager.getEntity(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a the IPN Polish Branches EAD, without preprocessing done";

        int origCount = getNodeCount(graph);

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream(BRANCH_2_XMLFILE)) {
            saxImportManager(EadImporter.class, EadHandler.class, "polishBranch.properties")
                    .importInputStream(ios, logMessage);
        }
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);


//        printGraph(graph);
        // How many new nodes will have been created? We should have
        /*
         * null: 4
         * relationship: 5
         * documentaryUnit: 3
         * documentDescription: 3
         * property: 1
         * maintenanceEvent: 1
         * systemEvent: 1
         * datePeriod: 2
         */

        int newCount = origCount + 20;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archdesc = graph.frame(
                getVertexByIdentifier(graph, BRANCH_2_ARCHDESC),
                DocumentaryUnit.class);
        DocumentaryUnit c1_1 = graph.frame(
                getVertexByIdentifier(graph, BRANCH_2_C01_1),
                DocumentaryUnit.class);
        DocumentaryUnit c1_2 = graph.frame(
                getVertexByIdentifier(graph, BRANCH_2_C01_2),
                DocumentaryUnit.class);

        // Check permission scope and hierarchy
        assertNull(archdesc.getParent());
        assertEquals(agent, archdesc.getRepository());
        assertEquals(agent, archdesc.getPermissionScope());
        assertEquals(archdesc, c1_1.getParent());
        assertEquals(archdesc, c1_1.getPermissionScope());
        assertEquals(archdesc, c1_2.getParent());
        assertEquals(archdesc, c1_2.getPermissionScope());


        //test titles
        Iterable<DocumentaryUnitDescription> descriptions = archdesc.getDocumentDescriptions();
        assertTrue(descriptions.iterator().hasNext());
        for (DocumentaryUnitDescription d : descriptions) {
            assertEquals("Collections from Biuro Udostępniania i Archiwizacji Dokumentów w Warszawie", d.getName());
            List<String> provenance = d.getProperty("processInfo");
            assertTrue(!provenance.isEmpty());
            assertThat(provenance.get(0), startsWith("This selection has been "));
        }
        for (DocumentaryUnitDescription desc : c1_1.getDocumentDescriptions()) {
            assertEquals("Areszt Śledczy Sądowy w Poznaniu [Untersuchungshaftanstalt Posen]", desc.getName());
        }
        //test hierarchy
        assertEquals(2, archdesc.countChildren());

        //test level-of-desc
        for (DocumentaryUnitDescription d : c1_1.getDocumentDescriptions()) {
            assertEquals("collection", d.getProperty("levelOfDescription"));
        }
        // test dates
        boolean hasDates = false;
        for (DocumentaryUnitDescription d : c1_1.getDocumentDescriptions()) {
            for (DatePeriod p : d.getDatePeriods()) {
                hasDates = true;
                assertEquals("1940-01-01", p.getStartDate());
                assertEquals("1943-12-31", p.getEndDate());
            }
        }
        assertTrue(hasDates);
    }
}
    
