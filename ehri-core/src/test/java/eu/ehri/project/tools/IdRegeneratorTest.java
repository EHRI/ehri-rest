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

package eu.ehri.project.tools;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.idgen.DescriptionIdGenerator;
import eu.ehri.project.models.idgen.IdGeneratorUtils;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.utils.Slugify;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class IdRegeneratorTest extends AbstractFixtureTest {

    private IdRegenerator idRegenerator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        idRegenerator = new IdRegenerator(graph);
    }

    @Test
    public void testReGenerateId() throws Exception {
        // It just so happens for this test that, for convenience reasons,
        // the fixtures do not have the IDs that their IdGenerators would
        // produce. So it's easy to test this.
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);
        Optional<List<String>> remap = idRegenerator.reGenerateId(doc);
        assertTrue(remap.isPresent());
        List<String> beforeAfter = remap.get();
        assertEquals(2, beforeAfter.size());
        assertEquals("c1", beforeAfter.get(0));
        assertEquals("nl-r1-c1", beforeAfter.get(1));
        // It shouldn't actually do anything by default...
        assertEquals("c1", doc.getId());
    }

    @Test(expected = IdRegenerator.IdCollisionError.class)
    public void testReGenerateIdWithCollision() throws Exception {
        DocumentaryUnit doc1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit doc2 = manager.getFrame("c4", DocumentaryUnit.class);
        // Give c4 its "natural" ID
        Optional<List<String>> regen = idRegenerator
                .withActualRename(true).reGenerateId(doc2);
        assertTrue(regen.isPresent());
        // Sneakily change the identifier property to trigger a collision
        manager.setProperty(doc1.asVertex(), Ontology.IDENTIFIER_KEY, "c4");
        idRegenerator.reGenerateId(doc1);
    }

    @Test
    public void testCollisionMode() throws Exception {
        DocumentaryUnit doc1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit doc2 = manager.getFrame("c4", DocumentaryUnit.class);
        // Give c4 its "natural" ID
        Optional<List<String>> regen = idRegenerator
                .withActualRename(true).reGenerateId(doc2);
        assertTrue(regen.isPresent());
        // Sneakily change the identifier property to trigger a collision
        manager.setProperty(doc1.asVertex(), Ontology.IDENTIFIER_KEY, "c4");
        Optional<List<String>> optionalCollision = idRegenerator
                .collisionMode(true)
                .reGenerateId(doc1);
        assertTrue(optionalCollision.isPresent());
    }

    @Test
    public void testReGenerateIdWSkippingCollisions() throws Exception {
        DocumentaryUnit doc1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit doc2 = manager.getFrame("c4", DocumentaryUnit.class);
        // Give c4 its "natural" ID
        Optional<List<String>> regen = idRegenerator
                .withActualRename(true)
                .reGenerateId(doc2);
        assertTrue(regen.isPresent());
        // Sneakily change the identifier property to trigger a collision
        manager.setProperty(doc1.asVertex(), Ontology.IDENTIFIER_KEY, "c4");
        assertFalse(idRegenerator
                .skippingCollisions(true).reGenerateId(doc1).isPresent());
    }

    @Test
    public void testReGenerateIdWithExplicitScope() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);
        Repository fakeScope = manager.getFrame("r2", Repository.class);
        Optional<List<String>> remap = idRegenerator.reGenerateId(fakeScope, doc);
        assertEquals("gb-r2-c1", remap.get().get(1));
    }

    @Test
    public void testReGenerateIdWithRename() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);
        Optional<List<String>> remap = idRegenerator.withActualRename(true).reGenerateId(doc);
        assertEquals("nl-r1-c1", remap.get().get(1));
        assertFalse(manager.exists("c1"));
        assertTrue(manager.exists("nl-r1-c1"));
        // It shouldn't actually do anything by default...
        assertEquals(remap.get().get(1), doc.getId());
        // The descriptions should also be renamed...
        DocumentaryUnitDescription desc = Iterables.getFirst(doc.getDocumentDescriptions(), null);
        assertNotNull(desc);
        String newDescId = String.format("%s%s%s%s%s",
                doc.getId(),
                DescriptionIdGenerator.DESCRIPTION_SEPARATOR,
                desc.getLanguageOfDescription(),
                IdGeneratorUtils.HIERARCHY_SEPARATOR,
                Slugify.slugify(desc.getDescriptionCode(), IdGeneratorUtils.SLUG_REPLACE));
        assertEquals(newDescId, desc.getId());
        // Doing it again should do nothing...
        assertFalse(idRegenerator.withActualRename(true)
                .reGenerateId(doc).isPresent());
    }

    @Test
    public void testReGenerateIds() throws Exception {
        Iterable<DocumentaryUnit> docs = manager.getFrames(EntityClass.DOCUMENTARY_UNIT,
                DocumentaryUnit.class);
        List<List<String>> remaps = idRegenerator.reGenerateIds(docs);
        assertEquals(4, remaps.size());
        Map<String,String> remap = Maps.newHashMap();
        for (List<String> rm : remaps) {
            remap.put(rm.get(0), rm.get(1));
        }
        assertEquals("nl-r1-c1", remap.get("c1"));
        assertEquals("nl-r1-c1-c2", remap.get("c2"));
        assertEquals("nl-r1-c1-c2-c3", remap.get("c3"));
        assertEquals("nl-r1-c4", remap.get("c4"));
    }
}