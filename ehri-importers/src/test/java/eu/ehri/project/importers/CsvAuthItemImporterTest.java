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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import java.io.InputStream;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class CsvAuthItemImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(CsvAuthItemImporterTest.class);
    protected final String SINGLE_EAD = "wien_victims.csv";

    @Test
    public void testImportItemsT() throws Exception {
        AuthoritativeSet authoritativeSet = manager.getFrame("auths", AuthoritativeSet.class);
        int voccount = toList(authoritativeSet.getAuthoritativeItems()).size();
        assertEquals(2, voccount);
        logger.debug("number of items: " + voccount);
        
        final String logMessage = "Importing some subjects";
      
        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        
         List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = new CsvImportManager(graph, authoritativeSet, validUser, CsvAuthoritativeItemImporter.class).importFile(ios, logMessage);
        printGraph(graph);
        
 // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
        System.out.println(Iterables.toList(authoritativeSet.getAuthoritativeItems()));
        /*
         * 9 Item
         * 9 ItemDesc
         * 10 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count+29, getNodeCount(graph));
        assertEquals(voccount + 9, toList(authoritativeSet.getAuthoritativeItems()).size());

        // Check permission scopes are correct.
        for (AccessibleEntity subject : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(authoritativeSet, subject.getPermissionScope());
        }
        
    }
}
