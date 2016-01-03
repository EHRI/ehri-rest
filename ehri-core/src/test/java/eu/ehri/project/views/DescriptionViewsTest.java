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

import com.google.common.base.Optional;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import eu.ehri.project.views.impl.CrudViews;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;


public class DescriptionViewsTest extends AbstractFixtureTest {

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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.IDENTIFIER_KEY, "some-new-id");

        DocumentaryUnitDescription changedDesc = descViews.create(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, validUser, Optional.<String>absent());
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        descViews.create(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, validUser, Optional.<String>absent());
        fail("Creating a dependent should have thrown a validation error");
    }

    @Test
    public void testUpdateDependent() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());
        Bundle descBundle = new Serializer(graph).entityToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.NAME_KEY, "some-new-title");

        DocumentaryUnitDescription changedDesc = descViews.update(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, validUser, Optional.<String>absent())
                .getNode();
        assertEquals(descCount, Iterables.count(unit.getDocumentDescriptions()));
        assertEquals("some-new-title", changedDesc.getName());
    }

    @Test(expected = ValidationError.class)
    public void testUpdateDependentWithValidationError() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        Bundle descBundle = new Serializer(graph).entityToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        descViews.update(unit.getId(), descBundle,
                DocumentaryUnitDescription.class, validUser, Optional.<String>absent()).getNode();
        fail("Updating a dependent should have thrown a validation error");
    }

    @Test
    public void testDeleteDependent() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());

        DocumentaryUnitDescription d = Iterables.first(unit.getDocumentDescriptions());
        assertNotNull(d);
        int delCount = descViews.delete(unit.getId(), d.getId(),
                validUser, Optional.<String>absent());
        Assert.assertTrue(delCount >= 1);
        assertEquals(descCount - 1, Iterables.count(unit.getDocumentDescriptions()));
    }

    @Test
    public void testDeleteDependentDescription() throws Exception {
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        // This should throw permission denied since c1 is not in the new item's subtree...
        int delete = descViews.delete("c1", "cd1", validUser, Optional.<String>absent());
        // the number of items deleted should be 1 desc, 2 dates, 2 access points = 5
        assertEquals(5, delete);
    }

    @Test
    public void testDeleteDependentAccessPoint() throws Exception {
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        // This should throw permission denied since c1 is not in the new item's subtree...
        int delete = descViews.delete("c1", "ur1", validUser, Optional.<String>absent());
        assertEquals(1, delete);
    }

    @Test(expected = PermissionDenied.class)
    public void testDeleteNonDependent() throws Exception {
        DescriptionViews<DocumentaryUnit> descViews = getView(DocumentaryUnit.class);
        // This should throw permission denied since c1 is not in the new item's subtree...
        descViews.delete("c1", "cd2", validUser, Optional.<String>absent());
    }

    @Test
    public void testUpdateDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        DescriptionViews<Repository> lcv = getView(Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.entityToBundle(description);
        Mutation<RepositoryDescription> cou = lcv.update(
                "r1", desc.withDataValue("name", "changed"),
                RepositoryDescription.class, validUser, Optional.<String>absent());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(cou.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
        assertNotSame(depSerializer.entityToBundle(description), old);
    }

    @Test
    public void testCreateDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Bundle desc = Bundle.fromData(TestData.getTestAgentBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
        DescriptionViews<Repository> lcv = getView(Repository.class);
        RepositoryDescription r11 = lcv.create("r1", desc, RepositoryDescription.class,
                validUser, Optional.<String>absent());
        assertEquals(r1, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testDeleteDependentLogging() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        DescriptionViews<Repository> lcv = getView(Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.entityToBundle(description);
        lcv.delete("r1", description.getId(), validUser, Optional.<String>absent());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
    }

    private <T  extends Described> DescriptionViews<T> getView(Class<T> cls) {
        return new DescriptionViews<>(graph, cls);
    }
}
