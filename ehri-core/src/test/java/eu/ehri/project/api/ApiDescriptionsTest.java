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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;


public class ApiDescriptionsTest extends AbstractFixtureTest {

    private ActionManager am;
    private Serializer depSerializer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        am = new ActionManager(graph);
        depSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    @Test
    public void testCreateDependent() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.IDENTIFIER_KEY, "some-new-id");

        DocumentaryUnitDescription changedDesc = api(adminUser)
                .createDependent(unit.getId(), descBundle,
                        DocumentaryUnitDescription.class, Optional.empty());
        unit.addDescription(changedDesc);

        // The order in which items are serialized is undefined, so we just have to throw
        // an error if we don't fine the right item...
        for (Bundle b : new Serializer(graph)
                .entityToBundle(unit).getRelations(Ontology
                        .DESCRIPTION_FOR_ENTITY)) {
            if (b.getDataValue(Ontology.IDENTIFIER_KEY).equals("some-new-id")) {
                return;
            }
        }
        fail("Item does not have description with identifier: some-new-id");
    }

    @Test(expected = ValidationError.class)
    public void testCreateDependentWithValidationError() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        api(adminUser).createDependent(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, Optional.empty());
        fail("Creating a dependent should have thrown a validation error");
    }

    @Test
    public void testUpdateDependent() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        int descCount = Iterables.size(unit.getDocumentDescriptions());
        Bundle descBundle = new Serializer(graph).entityToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.NAME_KEY, "some-new-title");

        DocumentaryUnitDescription changedDesc = api(adminUser)
                .updateDependent(unit.getId(), descBundle,
                        DocumentaryUnitDescription.class, Optional.empty())
                .getNode();
        assertEquals(descCount, Iterables.size(unit.getDocumentDescriptions()));
        assertEquals("some-new-title", changedDesc.getName());
    }

    @Test(expected = ValidationError.class)
    public void testUpdateDependentWithValidationError() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = new Serializer(graph).entityToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        api(adminUser).updateDependent(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, Optional.empty()).getNode();
        fail("Updating a dependent should have thrown a validation error");
    }

    @Test
    public void testDeleteDependent() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        int descCount = Iterables.size(unit.getDocumentDescriptions());

        DocumentaryUnitDescription d = Iterables.getFirst(unit.getDocumentDescriptions(), null);
        assertNotNull(d);
        int delCount = api(adminUser).deleteDependent(unit.getId(), d.getId(),
                Optional.empty());
        Assert.assertTrue(delCount >= 1);
        assertEquals(descCount - 1, Iterables.size(unit.getDocumentDescriptions()));
    }

    @Test
    public void testDeleteDependentDescription() throws Exception {
        // This should throw permission denied since c1 is not in the new item's subtree...
        int delete = api(adminUser).deleteDependent("c1", "cd1", Optional.empty());
        // the number of items deleted should be 1 desc, 2 dates, 2 access points = 5
        assertEquals(5, delete);
    }

    @Test
    public void testDeleteDependentAccessPoint() throws Exception {
        // This should throw permission denied since c1 is not in the new item's subtree...
        int delete = api(adminUser).deleteDependent("c1", "ur1", Optional.empty());
        assertEquals(1, delete);
    }

    @Test(expected = PermissionDenied.class)
    public void testDeleteNonDependent() throws Exception {
        // This should throw permission denied since c1 is not in the new item's subtree...
        api(adminUser).deleteDependent("c1", "cd2", Optional.empty());
    }

    @Test
    public void testUpdateDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle updated = depSerializer.entityToBundle(r1);
        Bundle desc = depSerializer.entityToBundle(description);
        Mutation<RepositoryDescription> cou = loggingApi(adminUser)
                .updateDependent("r1", desc.withDataValue("name", "changed"),
                        RepositoryDescription.class, Optional.empty());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(cou.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(updated, old);
        assertNotSame(depSerializer.entityToBundle(r1), old);
    }

    @Test
    public void testCreateDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Bundle desc = Bundle.fromData(TestData.getTestAgentBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
        loggingApi(adminUser).createDependent("r1", desc, RepositoryDescription.class,
                Optional.empty());
        assertEquals(r1, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testDeleteDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle updated = depSerializer.entityToBundle(r1);
        loggingApi(adminUser).deleteDependent("r1", description.getId(), Optional.empty());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(updated, old);
    }
}
