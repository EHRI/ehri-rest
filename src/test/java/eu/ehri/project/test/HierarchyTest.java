package eu.ehri.project.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;


import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships.Access;

public class HierarchyTest extends ModelTestBase {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCollectionHierarchy() {
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = helper.getTestFrame("c2", DocumentaryUnit.class);
        assertTrue(toList(c1.getChildren()).contains(c2));
        
        // check reverse
        assertEquals(c2.getParent(), c1);
    }
    
    @Test
    public void testCollectionAncestry() {
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = helper.getTestFrame("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = helper.getTestFrame("c3", DocumentaryUnit.class);
        // should be the first ancestor of c2
        assertEquals(toList(c2.getAncestors()).get(0), (c1));
        
        // and an ancestor of c3
        assertTrue(toList(c3.getAncestors()).contains(c1));
    }
    
    @Test
    public void testFullAncestry() {
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = helper.getTestFrame("c2", DocumentaryUnit.class);
        DocumentaryUnit c3 = helper.getTestFrame("c3", DocumentaryUnit.class);

        List<DocumentaryUnit> ancestors = toList(c3.getAncestors());
        assertEquals(2, ancestors.size());
        assertEquals(ancestors.get(0), c2);
        assertEquals(ancestors.get(1), c1);
    }
}
