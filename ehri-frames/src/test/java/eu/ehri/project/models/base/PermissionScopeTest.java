package eu.ehri.project.models.base;

import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PermissionScopeTest extends AbstractFixtureTest {
    public DocumentaryUnit doc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        doc = manager.getFrame("c2", DocumentaryUnit.class);
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        List<PermissionScope> scopes = Lists.newArrayList(
                manager.getFrame("c1", PermissionScope.class),
                manager.getFrame("r1", PermissionScope.class),
                manager.getFrame("nl", PermissionScope.class));
        assertEquals(scopes, Iterables.toList(doc.getPermissionScopes()));
    }

    @Test
    public void testIdChain() throws Exception {
        assertEquals(Lists.newArrayList("nl", "r1", "c1", "c2"), doc.idPath());
    }

    @Test
    public void testIdentifierIdRelationships() throws Exception {

        Bundle docBundle = Bundle.fromData(TestData.getTestDocBundle());
        Repository repo = manager.getFrame("r1", Repository.class);
        BundleDAO dao = new BundleDAO(graph, repo.idPath());
        DocumentaryUnit doc = dao.create(docBundle, DocumentaryUnit.class);
        assertEquals("nl-r1-someid-01", doc.getId());
        doc.setPermissionScope(repo);
        assertEquals(Lists.newArrayList("nl", "r1", "someid-01"), doc.idPath());
    }
}
