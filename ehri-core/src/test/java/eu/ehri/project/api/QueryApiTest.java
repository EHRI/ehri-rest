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

package eu.ehri.project.api;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryApiTest extends AbstractFixtureTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private QueryApi getQuery() {
        return api(validUser).query();
    }

    private QueryApi getQuery(Accessor accessor) {
        return api(accessor).query();
    }

    @Test
    public void testAdminCanListEverything() throws Exception {
        QueryApi query = getQuery();

        // Check we're not admin
        assertTrue(AclManager.belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // And the listing the ACL way...
        List<DocumentaryUnit> list = toList(query.page(DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());

        // Test the limit function
        QueryApi.Page<DocumentaryUnit> page = query.withLimit(1).page(DocumentaryUnit.class);
        list = toList(page);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());

        // Test the offset function
        list = toList(query.withLimit(2).withOffset(0).page(DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        // Test negative count (all items)
        list = toList(query.withLimit(-1).page(DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(5, list.size());

        // Test negative count (all items) and an offset
        list = toList(query.withOffset(1).withLimit(-1).page(DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(4, list.size());

        list = toList(query.withLimit(0).page(DocumentaryUnit.class));
        assertEquals(0, list.size());
    }

    @Test
    public void testPage() throws Exception {
        QueryApi query = getQuery();

        // Check we're not admin
        assertTrue(AclManager.belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Test the limit function
        QueryApi.Page<DocumentaryUnit> page = query.withLimit(1)
                .page(DocumentaryUnit.class);
        List<DocumentaryUnit> list = toList(page.getIterable());
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(toList(allDocs).size(), page.getTotal());
    }

    @Test
    public void testCount() throws Exception {
        assertEquals(5, getQuery().count(EntityClass.DOCUMENTARY_UNIT));
    }

    @Test
    public void testUserCannotListPrivate() throws Exception {
        // Check we're not admin
        Accessor accessor = manager.getEntity("reto", Accessor.class);
        QueryApi query = getQuery(accessor);

        DocumentaryUnit cantRead = manager
                .getEntity("c1", DocumentaryUnit.class);
        assertFalse(AclManager.belongsToAdmin(accessor));

        List<DocumentaryUnit> list = toList(query.page(DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertFalse(list.contains(cantRead));
    }

    @Test
    public void testListWithFilter() throws Exception {
        QueryApi query = getQuery();

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, "c1", DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testListWithStreaming() throws Exception {
        QueryApi query = getQuery().withStreaming(true);

        // Query for document identifier c1.
        QueryApi.Page<DocumentaryUnit> list = query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, "c1", DocumentaryUnit.class);
        assertEquals(-1, list.getTotal());
    }

    @Test
    public void testListWithPredicateFilter() throws Exception {
        QueryApi query = getQuery();

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.filter(
                Ontology.IDENTIFIER_KEY, QueryApi.FilterPredicate.EQUALS,
                "c1").page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(1, list.size());

        // Case-insensitive query
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.IEQUALS, "C1").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(1, list.size());

        // Startswith...
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.STARTSWITH, "c").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(4, list.size());

        // Endswith... should get one item (c1)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.ENDSWITH, "1").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(1, list.size());

        // Regexp... should get all doc units (c1-4)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.MATCHES, "^c\\d+$").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(4, list.size());

        // Less than... should get one item (c1)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.LT, "c2").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(1, list.size());
        assertEquals("c1", list.get(0).getIdentifier());

        // Greater than... should get one item (c4)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.GT, "c3").page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        list.sort(Comparator.comparing(Entity::getId));
        assertEquals(2, list.size());
        assertEquals("c4", list.get(0).getIdentifier());

        // Less than or equal... should get twos items (c1,c2)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.LTE, "c2")
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC)
                .page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(2, list.size());
        assertEquals("c1", list.get(0).getIdentifier());
        assertEquals("c2", list.get(1).getIdentifier());

        // Greater than or equal... should get two items (c3,c4)
        QueryApi fQuery = query.filter(Ontology.IDENTIFIER_KEY,
                QueryApi.FilterPredicate.GTE, "c3")
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC);
        list = toList(fQuery
                .page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertEquals(3, list.size());
        assertEquals("c3", list.get(0).getIdentifier());
        assertEquals("c4", list.get(1).getIdentifier());
        assertEquals("m19", list.get(2).getIdentifier());
        assertEquals(5, toList(fQuery.filter(Lists.<String>newArrayList())
                .page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class)).size());

    }

    @Test
    public void testListWithSort() throws Exception {
        QueryApi query = getQuery();

        // Query for document identifier c1.
        QueryApi.Page<DocumentaryUnit> page = query.orderBy(
                Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC).page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class);
        assertFalse(page.getTotal() == 0);
        assertEquals("c1", toList(page.getIterable()).get(0).getIdentifier());

        QueryApi orderQuery = query
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.DESC);
        page = orderQuery
                .page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class);
        assertFalse(page.getTotal() == 0);

        // NB: This will break if other collections are added to the
        // fixtures. Adjust as necessary.
        assertEquals("m19", toList(page.getIterable()).get(0).getIdentifier());
        assertEquals("c1", toList(orderQuery
                .filter(Lists.<String>newArrayList())
                .orderBy(Ontology.IDENTIFIER_KEY, QueryApi.Sort.ASC)
                .page(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class)
                .getIterable()).get(0).getIdentifier());
    }

    @Test
    public void testListWithGlobFilter() throws Exception {
        QueryApi query = getQuery();

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Query for document identifier starting with 'c'.
        // In the fixtures this is ALL docs
        List<DocumentaryUnit> list = toList(query.page(
                EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testListWithFailFilter() throws Exception {
        QueryApi query = getQuery();

        // Do a query that won't match anything.
        List<DocumentaryUnit> list = toList(query.page(
                Ontology.IDENTIFIER_KEY, "__GONNAFAIL__", DocumentaryUnit.class));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }
}
