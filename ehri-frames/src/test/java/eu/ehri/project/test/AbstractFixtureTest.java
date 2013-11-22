package eu.ehri.project.test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import org.junit.Before;
import org.junit.BeforeClass;

abstract public class AbstractFixtureTest extends ModelTestBase {

    // Members closely coupled to the test data!
    protected UserProfile validUser;
    protected UserProfile invalidUser;
    protected DocumentaryUnit item;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        try {
            item = manager.getFrame("c1", DocumentaryUnit.class);
            validUser = manager.getFrame("mike", UserProfile.class);
            invalidUser = manager.getFrame("reto", UserProfile.class);
        } catch (ItemNotFound e) {
            throw new RuntimeException(e);
        }
    }
}
