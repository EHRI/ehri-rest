package eu.ehri.project.models.cvoc;

import com.google.common.collect.Lists;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class VocabularyTest extends ModelTestBase {

    @Test
    public void testGetTopConcepts() throws Exception {
        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);
        List<Concept> top = Lists.newArrayList(vocabulary.getTopConcepts());
        List<Concept> all = Lists.newArrayList(vocabulary.getConcepts());
        assertEquals(1, top.size());
        assertEquals(top.get(0), manager.getEntity("cvocc1", Concept.class));
        assertEquals(2, all.size());
    }
}