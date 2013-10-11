package eu.ehri.project.test;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import org.junit.Before;
import org.junit.BeforeClass;

abstract public class AbstractFixtureTest extends ModelTestBase {

    // Members closely coupled to the test data!
    protected UserProfile validUser;
    protected UserProfile importUser;
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
        try {
            importUser = manager.getFrame("ehriimporter", UserProfile.class);
        } catch (ItemNotFound ex) {
            try {
                Bundle unit = new Bundle(EntityClass.USER_PROFILE)
                        .withDataValue(Ontology.IDENTIFIER_KEY, "ehriimporter")
                        .withDataValue(Ontology.NAME_KEY, "EHRI Importer");
                importUser =  new BundleDAO(graph, SystemScope.getInstance()).create(unit, UserProfile.class);
                Group admin = manager.getFrame("admin", Group.class);  // admin has id "admin"
                admin.addMember(importUser);
            } catch (ItemNotFound ex1) {
                throw new RuntimeException(ex1);
            } catch (ValidationError ex1) {
                throw new RuntimeException(ex1);
            }
        }
    }
}
