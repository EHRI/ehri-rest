package eu.ehri.project.importers.links;

import eu.ehri.project.models.base.Described;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinkResolverTest extends AbstractFixtureTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper.setInitializing(false).loadTestData("fixtures/link-resolver.yaml");
    }

    @Test
    public void solveUndeterminedRelationships() throws Exception {
        Described unit = manager.getEntity("c5", Described.class);
        LinkResolver linkResolver = new LinkResolver(graph, adminUser);
        int created = linkResolver.solveUndeterminedRelationships(unit);
        assertEquals(2, created);

        // running the same op again shouldn't create more links
        int created2 = linkResolver.solveUndeterminedRelationships(unit);
        assertEquals(0, created2);
    }
}