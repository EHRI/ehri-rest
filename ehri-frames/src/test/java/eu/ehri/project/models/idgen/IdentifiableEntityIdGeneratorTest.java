package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IdentifiableEntityIdGeneratorTest extends AbstractFixtureTest {

    private IdGenerator instance;
    private List<String> scopes;
    private Bundle bundle;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = IdentifiableEntityIdGenerator.INSTANCE;
        scopes = Lists.newArrayList("r1");
        bundle = Bundle.fromData(TestData.getTestDocBundle());
    }

    @Test()
    public void testHandleIdCollision() throws Exception {
        ListMultimap<String,String> errors = instance
                .handleIdCollision(scopes, bundle);
        assertTrue(errors.containsKey(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testGenerateIdWithStringScopes() throws Exception {
        String id = instance.generateId(scopes, bundle);
        assertEquals("r1-someid-01", id);

    }

    /**
     * If a child unit's ID includes the parent's unit ID, do not
     * duplicate. Tests {@link eu.ehri.project.models.idgen.IdGeneratorUtils.joinPath(Collection<String>)}
     *
     * @author Ben Companjen (https://github.com/bencomp)
     * @throws Exception
     */
    @Test
    public void testGenerateIdWithDupeStringScopes() throws Exception {
        List<String> ids = Lists.newArrayList("r1", "Fonds 1",
                "Fonds 1 / Subfonds 1", "Fonds 1 / Subfonds 1 / Item 3");
        String id = IdGeneratorUtils.joinPath(ids);
        assertEquals("r1-fonds-1-subfonds-1-item-3", id);
        
        List<String> ids2 = Lists.newArrayList("il-002798", "M.40", "M.40.MAP");
        String id2 = IdGeneratorUtils.joinPath(ids2);
        assertEquals("il-002798-m-40-map", id2);
        
        List<String> ids3 = Lists.newArrayList("de-002409", "DE ITS 1.1.0", "DE ITS 1.1.0.2", "2399000");
        String id3 = IdGeneratorUtils.joinPath(ids3);
        assertEquals("de-002409-de-its-1-1-0-2-2399000", id3);
        
        List<String> ids4 = Lists.newArrayList("cz-002279", "COLLECTION.JMP.SHOAH/T",
                "COLLECTION.JMP.SHOAH/T/2", "COLLECTION.JMP.SHOAH/T/2/A",
                "COLLECTION.JMP.SHOAH/T/2/A/1", "COLLECTION.JMP.SHOAH/T/2/A/1a",
                "COLLECTION.JMP.SHOAH/T/2/A/1a/028", "DOCUMENT.JMP.SHOAH/T/2/A/1a/028");
        String id4 = IdGeneratorUtils.joinPath(ids4);
        assertEquals("cz-002279-collection-jmp-shoah-t-2-a-1a-028-document-jmp-shoah-t-2-a-1a-028", id4);

    }

    @Test
    public void testGetIdBase() throws Exception {
        String id = instance.getIdBase(bundle);
        assertEquals("someid-01", id);
    }
}
