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

package eu.ehri.project.models.cvoc;

import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class ConceptTest extends AbstractFixtureTest {

    private Vocabulary v;
    private Concept cv1;
    private Concept cv2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        v = manager.getEntity("cvoc1", Vocabulary.class);
        cv1 = manager.getEntity("cvocc1", Concept.class);
        cv2 = manager.getEntity("cvocc2", Concept.class);
    }

    @Test
    public void testGetVocabulary() throws Exception {
        assertEquals(v, cv1.getVocabulary());
    }

    @Test
    public void testGetBroaderConcepts() throws Exception {
        assertEquals(cv1, cv2.getBroaderConcepts().iterator().next());
    }

    @Test
    public void testGetNarrowerConcepts() throws Exception {
        assertEquals(cv2, cv1.getNarrowerConcepts().iterator().next());
    }

    @Test
    public void testRemoveNarrowerConcept() throws Exception {
        cv1.removeNarrowerConcept(cv2);
        assertFalse(cv2.getNarrowerConcepts().iterator().hasNext());
    }

    @Test
    public void testGetRelatedConcepts() throws Exception {
        assertEquals(cv1, cv2.getRelatedConcepts().iterator().next());
        assertEquals(cv2, cv1.getRelatedByConcepts().iterator().next());
    }

    @Test
    public void testRemoveBroaderConcept() throws Exception {
        cv2.removeBroaderConcept(cv1);
        assertFalse(cv2.getBroaderConcepts().iterator().hasNext());
    }

    @Test
    public void testRemoveRelatedConcept() throws Exception {
        cv2.removeRelatedConcept(cv1);
        assertFalse(cv2.getRelatedConcepts().iterator().hasNext());
    }
}
