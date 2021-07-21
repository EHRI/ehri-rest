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

package eu.ehri.project.persistence;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.persistence.utils.DataUtils;
import eu.ehri.project.test.ModelTestBase;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BundleManagerTest extends ModelTestBase {

    private static final String ID = "c1";

    private Serializer serializer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        serializer = new Serializer(graph);
    }

    @Test
    public void testSerialisation() throws SerializationError,
            DeserializationError, ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        String json = serializer.entityToJson(c1);
        Bundle bundle = Bundle.fromString(json);
        assertEquals(ID, bundle.getId());

        // Test Repository serialization
        Repository r1 = manager.getEntity("r1", Repository.class);
        json = serializer.entityToJson(r1);
        bundle = Bundle.fromString(json);
        List<Bundle> descs = bundle.getRelations(Ontology.DESCRIPTION_FOR_ENTITY);
        assertEquals(1, descs.size());
        Bundle descBundle = descs.get(0);
        List<Bundle> addresses = descBundle
                .getRelations(Ontology.ENTITY_HAS_ADDRESS);
        assertEquals(1, addresses.size());
    }

    @Test
    public void testSaving() throws SerializationError, ValidationError,
            IntegrityError, ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        assertEquals(2, toList(c1.getDescriptions()).size());

        Bundle bundle = serializer.entityToBundle(c1);
        BundleManager persister = new BundleManager(graph);
        Mutation<DocumentaryUnit> c1redux = persister.update(bundle,
                DocumentaryUnit.class);

        assertEquals(toList(c1.getDescriptions()),
                toList(c1redux.getNode().getDescriptions()));
    }

    @Test
    public void testSavingAgent() throws SerializationError, ValidationError,
            IntegrityError, ItemNotFound {
        Repository r1 = manager.getEntity("r1", Repository.class);
        assertEquals(1, toList(r1.getDescriptions()).size());

        Bundle bundle = serializer.entityToBundle(r1);
        BundleManager persister = new BundleManager(graph);
        Mutation<Repository> r1redux = persister.update(bundle, Repository.class);

        assertEquals(toList(r1.getDescriptions()),
                toList(r1redux.getNode().getDescriptions()));

        RepositoryDescription ad1 = graph.frame(r1redux.getNode().getDescriptions().iterator()
                .next().asVertex(), RepositoryDescription.class);
        assertEquals(1, toList(ad1.getAddresses()).size());
    }

    @Test
    public void testSavingWithDependentChanges() throws SerializationError,
            DeserializationError, ValidationError, IntegrityError, ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        assertEquals(2, toList(c1.getDescriptions()).size());
        String json = serializer.vertexToJson(c1.asVertex());

        Description desc = toList(c1.getDescriptions()).get(0);
        c1.removeDescription(desc);
        assertEquals(1, toList(c1.getDescriptions()).size());

        // Restore the item from JSON
        Bundle bundle = Bundle.fromString(json);
        BundleManager persister = new BundleManager(graph);
        persister.update(bundle, DocumentaryUnit.class);

        // Our deleted description should have come back...
        assertEquals(2, toList(c1.getDescriptions()).size());
    }

    @Test
    public void testDeletingDependents() throws SerializationError, ValidationError, IntegrityError, ItemNotFound, InterruptedException {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        DocumentaryUnitDescription cd1 = manager.getEntity("cd1", DocumentaryUnitDescription.class);
        Bundle bundle = new Serializer(graph).entityToBundle(cd1);
        assertEquals(2, Iterables.size(c1.getDocumentDescriptions()));
        assertEquals(2, Iterables.size(cd1.getDatePeriods()));

        String deletePath = "hasDate[0]";
        String dpId = DataUtils.getItem(bundle, deletePath).getId();
        try {
            manager.getEntity(dpId, DatePeriod.class);
        } catch (ItemNotFound e) {
            fail("Date period '" + dpId + "' not found in index before delete test.");
        }

        // Delete the *second* date period from the first description...
        Bundle newBundle = DataUtils.deleteItem(bundle, deletePath);
        BundleManager persister = new BundleManager(graph);
        Mutation<DocumentaryUnit> mutation = persister.update(newBundle, DocumentaryUnit.class);

        assertEquals(MutationState.UPDATED, mutation.getState());
        assertEquals(2, Iterables.size(c1.getDocumentDescriptions()));
        assertEquals(1, Iterables.size(manager.getEntity("cd1", DocumentaryUnitDescription.class).getDatePeriods()));

        // The second date period should be gone from the index
        try {
            DatePeriod e = manager.getEntity(dpId, DatePeriod.class);
            fail("Date period '" + dpId + "' found in index AFTER delete test.");
        } catch (ItemNotFound e) {
            // No problem
        }

        // It should also not exist as a node...
        try {
            graph.getVertices(EntityType.ID_KEY, dpId).iterator().next();
            fail("Date period '" + dpId + "' found in index AFTER delete test.");
        } catch (NoSuchElementException e) {
            // No problem
        }
    }

    @Test(expected = ItemNotFound.class)
    public void testDeletingWholeBundle() throws SerializationError,
            ValidationError, ItemNotFound {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        DocumentaryUnitDescription cd1 = manager.getEntity("cd1", DocumentaryUnitDescription.class);
        Bundle bundle = serializer.entityToBundle(c1);
        assertEquals(2, toList(cd1.getDatePeriods()).size());
        List<DatePeriod> dates = toList(manager.getEntities(
                EntityClass.DATE_PERIOD, DatePeriod.class));

        BundleManager persister = new BundleManager(graph);
        Integer numDeleted = persister.delete(bundle);
        assertTrue(numDeleted > 0);
        assertEquals(
                dates.size() - 2,
                toList(
                        manager.getEntities(EntityClass.DATE_PERIOD,
                                DatePeriod.class)).size());
        // Should raise NoSuchElementException
        manager.getEntity(ID, DocumentaryUnit.class);
    }

    @Test(expected = ValidationError.class)
    public void testValidationError() throws SerializationError,
            ValidationError, ItemNotFound, IntegrityError {
        DocumentaryUnit c1 = manager.getEntity(ID, DocumentaryUnit.class);
        Bundle bundle = serializer.entityToBundle(c1);
        Bundle desc = DataUtils.getItem(bundle, "describes[0]");
        Bundle newBundle = desc.removeDataValue(Ontology.NAME_KEY);

        BundleManager persister = new BundleManager(graph);
        persister.update(newBundle, DocumentaryUnit.class);
        fail("Bundle with no description name did not throw a ValidationError");
    }

    @Test(expected = ValidationError.class)
    public void testUpdateWithNoIdentifier() throws SerializationError,
            ValidationError, ItemNotFound, IntegrityError, DeserializationError {
        Bundle b1 = Bundle.fromData(TestData.getTestAgentBundle())
                .removeDataValue(Ontology.IDENTIFIER_KEY);

        BundleManager persister = new BundleManager(graph);
        persister.update(b1, Repository.class);
        fail("Attempting to update a non-existent bundle did not throw an error");
    }

    @Test
    public void testCreationWithUnicodeIdentifier() throws Exception {
        Bundle b1 = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, "foo /?&% ארכיו bar");
        DocumentaryUnit doc = new BundleManager(graph).create(b1, DocumentaryUnit.class);
        assertEquals("foo_ארכיו_bar", doc.getId());
    }
}
