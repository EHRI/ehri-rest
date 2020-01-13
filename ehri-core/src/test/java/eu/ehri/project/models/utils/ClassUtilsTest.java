/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.models.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ClassUtilsTest {

    @Test
    public void testGetEntityType() throws Exception {
        assertEquals(EntityClass.DOCUMENTARY_UNIT,
                ClassUtils.getEntityType(DocumentaryUnit.class));
    }

    @Test
    public void testGetDependentRelations() throws Exception {
        Map<String, Direction> deps = Maps.newHashMap();
        deps.put(Ontology.DESCRIPTION_FOR_ENTITY, Direction.IN);
        assertEquals(deps, ClassUtils.getDependentRelations(DocumentaryUnit.class));
    }

    @Test
    public void testGetFetchMethods() throws Exception {
        Set<String> fetch = Sets.newHashSet(
                Ontology.DESCRIPTION_FOR_ENTITY,
                Ontology.DOC_HELD_BY_REPOSITORY,
                Ontology.DOC_IS_CHILD_OF,
                Ontology.ENTITY_HAS_LIFECYCLE_EVENT,
                Ontology.IS_ACCESSIBLE_TO
        );
        assertEquals(fetch, ClassUtils.getFetchMethods(DocumentaryUnit.class).keySet());
    }

    @Test
    public void testGetPropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet(
                Ontology.IDENTIFIER_KEY,
                EntityType.ID_KEY,
                EntityType.TYPE_KEY
        );
        assertEquals(keys, ClassUtils.getPropertyKeys(DocumentaryUnit.class));
    }

    @Test
    public void testGetMandatoryPropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet(
                Ontology.IDENTIFIER_KEY
        );
        assertEquals(keys, ClassUtils.getMandatoryPropertyKeys(DocumentaryUnit.class));
    }

    @Test
    public void testGetUniquePropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet();
        assertEquals(keys, ClassUtils.getUniquePropertyKeys(DocumentaryUnit.class));
    }

    @Test
    public void testGetIndexedPropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet(
                Ontology.DATE_PERIOD_START_DATE,
                Ontology.DATE_PERIOD_END_DATE,
                Ontology.DATE_PERIOD_TYPE
        );
        assertEquals(keys, ClassUtils.getIndexedPropertyKeys(DatePeriod.class));
    }

    @Test
    public void testGetEnumMethods() throws Exception {
        Map<String, Set<String>> enumPropertyKeys = ClassUtils.getEnumPropertyKeys(AccessPoint.class);
        Set<String> values = enumPropertyKeys.get(Ontology.ACCESS_POINT_TYPE);
        assertNotNull(values);
        assertFalse(values.isEmpty());
    }
}