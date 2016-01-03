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

package eu.ehri.project.views;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.views.Query.Page;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryTest extends AbstractFixtureTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testAdminCanListEverything() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        assertTrue(AclManager.belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // And the listing the ACL way...
        List<DocumentaryUnit> list = toList(query.page(validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());

        // Test the limit function
        Page<DocumentaryUnit> page = query.setLimit(1).page(validUser);
        list = toList(page);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());

        // Test the offset function
        list = toList(query.setLimit(2).setOffset(0).page(validUser));
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        // Test negative count (all items)
        list = toList(query.setLimit(-1).page(validUser));
        assertFalse(list.isEmpty());
        assertEquals(5, list.size());

        // Test negative count (all items) and an offset
        list = toList(query.setOffset(1).setLimit(-1).page(validUser));
        assertFalse(list.isEmpty());
        assertEquals(4, list.size());

        list = toList(query.setLimit(0).page(validUser));
        assertEquals(0, list.size());
    }

    @Test
    public void testPage() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        assertTrue(AclManager.belongsToAdmin(validUser));

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Test the limit function
        Query.Page<DocumentaryUnit> page = query.setLimit(1).page(validUser);
        List<DocumentaryUnit> list = toList(page.getIterable());
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(toList(allDocs).size(), page.getTotal());
    }

    @Test
    public void testCount() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph, DocumentaryUnit.class);
        assertEquals(5, query.count());
    }

    @Test
    public void testUserCannotListPrivate() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Check we're not admin
        Accessor accessor = manager.getEntity("reto", Accessor.class);
        DocumentaryUnit cantRead = manager
                .getEntity("c1", DocumentaryUnit.class);
        assertFalse(AclManager.belongsToAdmin(accessor));

        List<DocumentaryUnit> list = toList(query.page(accessor));
        assertFalse(list.isEmpty());
        assertFalse(list.contains(cantRead));
    }

    @Test
    public void testListWithFilter() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.setLimit(1).page(
                Ontology.IDENTIFIER_KEY, "c1", validUser));
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testListWithStreaming() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class).setStream(true);

        // Query for document identifier c1.
        Page<DocumentaryUnit> list = query.setLimit(1).page(
                Ontology.IDENTIFIER_KEY, "c1", validUser);
        assertEquals(-1L, list.getTotal());
    }

    @Test
    public void testListWithDepthFilter() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Query for only top-level documentary units.
        // The result should be c1 and c4
        List<DocumentaryUnit> list = toList(query.depthFilter(
                Ontology.DOC_IS_CHILD_OF, Direction.OUT, 0).page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());

        // The same query with a depth filter of 1 should get 3 items
        list = toList(query.depthFilter(Ontology.DOC_IS_CHILD_OF,
                Direction.OUT, 1).page(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(4, list.size());

    }

    @Test
    public void testListWithPredicateFilter() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        List<DocumentaryUnit> list = toList(query.filter(
                Ontology.IDENTIFIER_KEY, Query.FilterPredicate.EQUALS,
                "c1").page(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());

        // Case-insensitive query
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.IEQUALS, "C1").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());

        // Startswith...
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.STARTSWITH, "c").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(4, list.size());

        // Endswith... should get one item (c1)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.ENDSWITH, "1").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());

        // Regexp... should get all doc units (c1-4)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.MATCHES, "^c\\d+$").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(4, list.size());

        // Less than... should get one item (c1)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.LT, "c2").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(1, list.size());
        assertEquals("c1", list.get(0).getIdentifier());

        // Greater than... should get one item (c4)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.GT, "c3").page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(2, list.size());
        assertEquals("c4", list.get(0).getIdentifier());

        // Less than or equal... should get twos items (c1,c2)
        list = toList(query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.LTE, "c2")
                .orderBy(Ontology.IDENTIFIER_KEY, Query.Sort.ASC)
                .page(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(2, list.size());
        assertEquals("c1", list.get(0).getIdentifier());
        assertEquals("c2", list.get(1).getIdentifier());

        // Greater than or equal... should get two items (c3,c4)
        Query<DocumentaryUnit> fQuery = query.filter(Ontology.IDENTIFIER_KEY,
                Query.FilterPredicate.GTE, "c3")
                .orderBy(Ontology.IDENTIFIER_KEY, Query.Sort.ASC);
        list = toList(fQuery
                .page(EntityClass.DOCUMENTARY_UNIT, validUser));
        assertEquals(3, list.size());
        assertEquals("c3", list.get(0).getIdentifier());
        assertEquals("c4", list.get(1).getIdentifier());
        assertEquals("m19", list.get(2).getIdentifier());
        assertEquals(5, toList(fQuery.clearFilters()
                .page(EntityClass.DOCUMENTARY_UNIT, validUser)).size());

    }

    @Test
    public void testListWithTraversalFilter() {
        Query<DocumentaryUnit> query1 = new Query<>(graph,
                DocumentaryUnit.class);
        List<String> filters1 = ImmutableList.of(
          "<-describes.identifier:c1-desc"
        );
        Iterable<DocumentaryUnit> list1 = query1
                .filter(filters1).page(validUser);
        assertEquals(1, Iterables.size(list1));

        Query<Repository> query2 = new Query<>(graph,
                Repository.class);
        List<String> filters2 = ImmutableList.of(
                "<-describes->hasAddress.municipality:Brussels"
        );
        Iterable<Repository> list2 = query2
                .filter(filters2).page(validUser);
        assertEquals(1, Iterables.size(list2));

    }

    @Test
    public void testListWithTraversalOrder() {
        Query<Repository> query1 = new Query<>(graph, Repository.class);
        Iterable<Repository> out1 = query1
                .orderBy(ImmutableList.of("<-describes.identifier"))
                .page(validUser);
        List<Repository> list1 = Lists.newLinkedList(out1);

        Query<Repository> query2 = new Query<>(graph, Repository.class);
        Iterable<Repository> out2 = query2
                .orderBy(ImmutableList.of("<-describes.identifier__DESC"))
                .page(validUser);
        List<Repository> list2 = Lists.newLinkedList(out2);

        assertEquals(list1, Lists.reverse(list2));
    }


    @Test
    public void testListWithSort() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Query for document identifier c1.
        Page<DocumentaryUnit> page = query.orderBy(
                Ontology.IDENTIFIER_KEY, Query.Sort.ASC).page(
                EntityClass.DOCUMENTARY_UNIT, validUser);
        assertFalse(page.getTotal() == 0);
        assertEquals("c1", toList(page.getIterable()).get(0).getIdentifier());

        Query<DocumentaryUnit> orderQuery = query
                .orderBy(Ontology.IDENTIFIER_KEY, Query.Sort.DESC);
        page = orderQuery
                .page(EntityClass.DOCUMENTARY_UNIT, validUser);
        assertFalse(page.getTotal() == 0);

        // NB: This will break if other collections are added to the
        // fixtures. Adjust as necessary.
        assertEquals("m19", toList(page.getIterable()).get(0).getIdentifier());
//        assertEquals("c4", toList(page.getIterable()).get(1).getIdentifier());
        assertEquals("c1", toList(orderQuery
                .clearOrdering()
                .defaultOrderBy(Ontology.IDENTIFIER_KEY, Query.Sort.ASC)
                .page(EntityClass.DOCUMENTARY_UNIT, validUser)
                .getIterable()).get(0).getIdentifier());
    }

    @Test
    public void testListWithGlobFilter() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Get the total number of DocumentaryUnits the old-fashioned way
        Iterable<Vertex> allDocs = manager
                .getVertices(EntityClass.DOCUMENTARY_UNIT);

        // Query for document identifier starting with 'c'.
        // In the fixtures this is ALL docs
        List<DocumentaryUnit> list = toList(query.page(
                EntityClass.DOCUMENTARY_UNIT, validUser));
        assertFalse(list.isEmpty());
        assertEquals(toList(allDocs).size(), list.size());
    }

    @Test
    public void testListWithFailFilter() throws Exception {
        Query<DocumentaryUnit> query = new Query<>(graph,
                DocumentaryUnit.class);

        // Do a query that won't match anything.
        List<DocumentaryUnit> list = toList(query.page(
                Ontology.IDENTIFIER_KEY, "__GONNAFAIL__", validUser));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }
}
