package eu.ehri.project.views.impl;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.RepositoryDescription;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.Mutation;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LoggingCrudViewsTest extends AbstractFixtureTest {

    private ActionManager am;
    private Serializer depSerializer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        am = new ActionManager(graph);
        depSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    @Test
    public void testCreate() throws Exception {
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = lcv.create(repoBundle, validUser);
        assertEquals(repository, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        Bundle before = depSerializer.vertexFrameToBundle(manager.getFrame("r1", Repository.class));
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle())
                .withId("r1");
        Mutation<Repository> cou = lcv.createOrUpdate(repoBundle, validUser);
        assertTrue(cou.updated());
        SystemEvent event = am.getLatestGlobalEvent();
        assertEquals(cou.getNode(), event.getSubjects().iterator().next());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertNotSame(old, repoBundle);
        Assert.assertEquals(before, old);
    }

    @Test
    public void testUpdate() throws Exception {
        Bundle before = depSerializer.vertexFrameToBundle(manager.getFrame("r1", Repository.class));
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        Mutation<Repository> cou = lcv.update(before.withDataValue("identifier", "new-id"), validUser);
        assertTrue(cou.updated());
        SystemEvent event = am.getLatestGlobalEvent();
        assertEquals(cou.getNode(), event.getSubjects().iterator().next());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(before, old);
    }

    @Test
    public void testUpdateDependent() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.vertexFrameToBundle(description);
        Mutation<RepositoryDescription> cou = lcv.updateDependent(
                desc.withDataValue("name", "changed"),
                r1, validUser, RepositoryDescription.class);
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(cou.updated());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
        assertNotSame(depSerializer.vertexFrameToBundle(description), old);
    }

    @Test
    public void testCreateDependent() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        Bundle desc = Bundle.fromData(TestData.getTestAgentBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        RepositoryDescription added = lcv
                .createDependent(desc,
                        r1, validUser, RepositoryDescription.class);
        assertEquals(r1, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testDelete() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        Bundle before = depSerializer.vertexFrameToBundle(r1);
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        lcv.delete(r1, validUser);
        SystemEvent event = am.getLatestGlobalEvent();
        assertFalse(manager.exists("r1"));
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(before, old);
    }

    @Test
    public void testDeleteDependent() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        Crud<Repository> lcv = ViewFactory.getCrudWithLogging(graph, Repository.class);
        Description description = r1.getDescriptions().iterator().next();
        Bundle desc = depSerializer.vertexFrameToBundle(description);
        lcv.deleteDependent(description, r1, validUser, Description.class);
        SystemEvent event = am.getLatestGlobalEvent();
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(desc, old);
    }
}
