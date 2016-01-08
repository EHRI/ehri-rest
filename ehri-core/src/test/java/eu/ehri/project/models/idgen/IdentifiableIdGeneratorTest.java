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

package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class IdentifiableIdGeneratorTest extends AbstractFixtureTest {

    private IdGenerator instance;
    private List<String> scopes;
    private Bundle bundle;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = IdentifiableEntityIdGenerator.INSTANCE;
        scopes = Lists.newArrayList("r1");
        bundle = Bundle.fromData(TestData.getTestDocBundle());
    }

    @Test()
    public void testHandleIdCollision() throws Exception {
        ListMultimap<String, String> errors = instance
                .handleIdCollision(scopes, bundle);
        assertTrue(errors.containsKey(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testGenerateIdWithStringScopes() throws Exception {
        String id = instance.generateId(scopes, bundle);
        assertEquals("r1-someid_01", id);

    }

    @Test
    public void testGenerateIdWithPartialDupeStringScopes() throws Exception {
        List<String> ids = Lists.newArrayList("r1", "Fonds 1",
                "Fonds 1 / Subfonds 1", "Fonds 1 / Subfonds 1 / Item 3");
        String id = IdGeneratorUtils.joinPath(ids);
        assertEquals("r1-fonds_1-subfonds_1-item_3", id);

        List<String> ids2 = Lists.newArrayList("il-002798", "M.40", "M.40.MAP");
        String id2 = IdGeneratorUtils.joinPath(ids2);
        assertEquals("il_002798-m_40-map", id2);

        List<String> ids3 = Lists.newArrayList("de-002409", "DE ITS 1.1.0", "DE ITS 1.1.0.2", "2399000");
        String id3 = IdGeneratorUtils.joinPath(ids3);
        assertEquals("de_002409-de_its_1_1_0-2-2399000", id3);

        List<String> ids4 = Lists.newArrayList("cz-002279", "COLLECTION.JMP.SHOAH/T",
                "COLLECTION.JMP.SHOAH/T/2", "COLLECTION.JMP.SHOAH/T/2/A",
                "COLLECTION.JMP.SHOAH/T/2/A/1", "COLLECTION.JMP.SHOAH/T/2/A/1a",
                "COLLECTION.JMP.SHOAH/T/2/A/1a/028", "DOCUMENT.JMP.SHOAH/T/2/A/1a/028");
        String id4 = IdGeneratorUtils.joinPath(ids4);
        // special prefixes are not treated in 'smart' way
        assertEquals("cz_002279-collection_jmp_shoah_t-2-a-1-a-028-document_jmp_shoah_t_2_a_1a_028", id4);

        // The check is case sensitive, so prefix is repeated if case is different
        List<String> ids5 = Lists.newArrayList("de-002409", "DE ITS 1.1.0", "de ITS 1.1.0.2", "2399000");
        String id5 = IdGeneratorUtils.joinPath(ids5);
        assertEquals("de_002409-de_its_1_1_0-de_its_1_1_0_2-2399000", id5);

    }

    @Test
    public void testGenerateIdWithDupeStringScopes() throws Exception {
        List<String> ids = Lists.newArrayList("r1", "Fonds 1",
                "Thing 1", "Thing 1", "Thing 2");
        String id = IdGeneratorUtils.joinPath(ids);
        assertEquals("r1-fonds_1-thing_1-thing_1-thing_2", id);

        List<String> ids2 = Lists.newArrayList("il-002798", "M.40", "MAP", "map");
        String id2 = IdGeneratorUtils.joinPath(ids2);
        assertEquals("il_002798-m_40-map-map", id2);

        List<String> ids3 = Lists.newArrayList("de-002409", "DE ITS 1.1.0", "1.1.0", "1.1.0", "2399000");
        String id3 = IdGeneratorUtils.joinPath(ids3);
        assertEquals("de_002409-de_its_1_1_0-1_1_0-1_1_0-2399000", id3);
    }

    @Test
    public void testHierarchyCollisions() throws Exception {
        List<String> path1 = Lists.newArrayList("r1", "1-a", "b");
        List<String> path2 = Lists.newArrayList("r1", "1", "a-b");
        assertNotEquals(IdGeneratorUtils.joinPath(path1),
                IdGeneratorUtils.joinPath(path2));
    }

    @Test
    public void testGetIdBase() throws Exception {
        String id = instance.getIdBase(bundle);
        assertEquals("someid-01", id);
    }
}
