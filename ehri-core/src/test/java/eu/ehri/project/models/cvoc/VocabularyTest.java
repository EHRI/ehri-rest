/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.cvoc;

import com.google.common.collect.Lists;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class VocabularyTest extends ModelTestBase {

    @Test
    public void testGetTopConcepts() throws Exception {
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        List<Concept> top = Lists.newArrayList(vocabulary.getTopConcepts());
        List<Concept> all = Lists.newArrayList(vocabulary.getConcepts());
        assertEquals(1, top.size());
        assertEquals(top.get(0), manager.getEntity("cvocc1", Concept.class));
        assertEquals(2, all.size());
    }

    @Test
    public void testMetadataSerialization() throws Exception {
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        Serializer serializer = new Serializer(graph).withDependentOnly(true);
        Bundle bundle = serializer.entityToBundle(vocabulary);
        assertEquals(2, bundle.getMetaData().get(ItemHolder.CHILD_COUNT));
    }
}