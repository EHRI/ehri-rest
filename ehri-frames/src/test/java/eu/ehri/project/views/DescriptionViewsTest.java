package eu.ehri.project.views;

import com.google.common.base.Optional;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
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
import eu.ehri.project.views.impl.CrudViews;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
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
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews
                = new DescriptionViews<DocumentaryUnit>(graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.IDENTIFIER_KEY, "some-new-id");

        DocumentDescription changedDesc = descViews.create(unit.getId(), descBundle,
                DocumentDescription.class, validUser, Optional.<String>absent());
        unit.addDescription(changedDesc);

        // The order in which items are serialized is undefined, so we just have to throw
        // an error if we don't fine the right item...
        for (Bundle b : new Serializer(graph)
                .vertexFrameToBundle(unit).getRelations(Ontology
                        .DESCRIPTION_FOR_ENTITY)) {
            if (b.getDataValue(Ontology.IDENTIFIER_KEY).equals("some-new-id")) {
                return;
            }
        }
        fail("Item does not have description with identifier: some-new-id");
    }

    @Test(expected = ValidationError.class)
    public void testCreateDependentWithValidationError() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews
                = new DescriptionViews<DocumentaryUnit>(graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        Bundle descBundle = bundle
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        descViews.create(unit.getId(), descBundle,
                DocumentDescription.class, validUser, Optional.<String>absent());
        fail("Creating a dependent should have thrown a validation error");
    }

    @Test
    public void testUpdateDependent() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews
                = new DescriptionViews<DocumentaryUnit>(graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());
        Bundle descBundle = new Serializer(graph).vertexFrameToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).withDataValue(Ontology.NAME_KEY, "some-new-title");

        DocumentDescription changedDesc = descViews.update(unit.getId(), descBundle,
                DocumentDescription.class, validUser, Optional.<String>absent())
                .getNode();
        assertEquals(descCount, Iterables.count(unit.getDocumentDescriptions()));
        assertEquals("some-new-title", changedDesc.getName());
    }

    @Test(expected = ValidationError.class)
    public void testUpdateDependentWithValidationError() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews
                = new DescriptionViews<DocumentaryUnit>(graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        Bundle descBundle = new Serializer(graph).vertexFrameToBundle(unit)
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY)
                .get(0).removeDataValue(Ontology.NAME_KEY);

        descViews.update(unit.getId(), descBundle,
                DocumentDescription.class, validUser, Optional.<String>absent()).getNode();
        fail("Updating a dependent should have thrown a validation error");
    }

    @Test
    public void testDeleteDependent() throws Exception {
        CrudViews<DocumentaryUnit> docViews = new CrudViews<DocumentaryUnit>(
                graph, DocumentaryUnit.class);
        DescriptionViews<DocumentaryUnit> descViews
                = new DescriptionViews<DocumentaryUnit>(graph, DocumentaryUnit.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = docViews.create(bundle, validUser);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.asVertex().getProperty("name"));

        long descCount = Iterables.count(unit.getDocumentDescriptions());

        DocumentDescription d = Iterables.first(unit.getDocumentDescriptions());
        assertNotNull(d);
        int delCount = descViews.delete(unit.getId(), d.getId(),
                validUser, Optional.<String>absent());
        Assert.assertTrue(delCount >= 1);
        assertEquals(descCount - 1, Iterables.count(unit.getDocumentDescriptions()));
    }


    @Test
    public void testUpdateDependentLogging() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        DescriptionViews<Repository> lcv = new DescriptionViews<Repository>(graph, Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.vertexFrameToBundle(description);
        Mutation<RepositoryDescription> cou = lcv.update(
                "r1", desc.withDataValue("name", "changed"),
                RepositoryDescription.class, validUser, Optional.<String>absent());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(cou.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
        assertNotSame(depSerializer.vertexFrameToBundle(description), old);
    }

    @Test
    public void testCreateDependentLogging() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        Bundle desc = Bundle.fromData(TestData.getTestAgentBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
        DescriptionViews<Repository> lcv = new DescriptionViews<Repository>(graph, Repository.class);
        RepositoryDescription r11 = lcv.create("r1", desc, RepositoryDescription.class,
                validUser, Optional.<String>absent());
        assertEquals(r11, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testDeleteDependentLogging() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        DescriptionViews<Repository> lcv = new DescriptionViews<Repository>(graph, Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.vertexFrameToBundle(description);
        lcv.delete("r1", description.getId(), validUser, Optional.<String>absent());
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
    }
}
