package eu.ehri.project.importers.links;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.utils.Table;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class LinkImporterTest extends AbstractFixtureTest {

    private final Table goodData = Table.of(ImmutableList.of(
            ImmutableList.of("r1", "c1", "associative", "", "Test"),
            ImmutableList.of("r4", "c4", "associative", "", "Test 2")
    ));

    private final Table badData = Table.of(new ImmutableList.Builder<List<String>>()
            .addAll(goodData.rows())
            .add(ImmutableList.of("BAD", "c4", "associative", "", "Test 2")).build());


    @Test
    public void importLinks() throws Exception {

        ImportLog log = new LinkImporter(graph, validUser, false)
                .importLinks(goodData, "testing");
        assertEquals(2, log.getCreated());
    }

    @Test(expected = DeserializationError.class)
    public void importLinksWithMissingTarget() throws Exception {
        new LinkImporter(graph, validUser, false)
                .importLinks(badData, "testing");
    }

    @Test
    public void importLinksWithMissingTargetInTolerantMode() throws Exception {
        ImportLog log = new LinkImporter(graph, validUser, true)
                .importLinks(badData, "testing");
        assertEquals(2, log.getCreated());
    }
}