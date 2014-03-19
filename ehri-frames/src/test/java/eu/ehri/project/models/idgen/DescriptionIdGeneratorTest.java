package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DescriptionIdGeneratorTest extends AbstractFixtureTest {

    private IdGenerator instance;
    private List<String> scopes;
    private Bundle bundle;

    @Override
    @Before
    public void setUp() throws Exception{
        super.setUp();
        instance = DescriptionIdGenerator.INSTANCE;
        scopes = Lists.newArrayList("nl", "r1", "c1");
        bundle = Bundle.fromData(TestData.getTestDocBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
    }

    @Test()
    public void testHandleIdCollision() throws Exception {
        ListMultimap<String,String> errors = instance.handleIdCollision(scopes, bundle);
        assertTrue(errors.containsKey(Ontology.LANGUAGE));
    }

    @Test
    public void testGenerateIdWithStringScopes() throws Exception {
        String id = instance.generateId(scopes, bundle);
        assertEquals("nl-r1-c1-eng-someid-01", id);

    }

    @Test
    public void testGetIdBase() throws Exception {
        String id = instance.getIdBase(bundle);
        assertEquals("eng-someid-01", id);
    }
}
