package eu.ehri.project.tools;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FindReplaceTest extends AbstractFixtureTest {

    @Test
    public void testFindAndReplace() throws Exception {
        FindReplace findReplace = new FindReplace(graph, true, 100);
        List<VertexProxy> before = getGraphState(graph);
        List<Accessible> done = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.REPOSITORY_DESCRIPTION,
                "name", "Description",
                "Test", validUser, "This is a test");
        List<VertexProxy> after = getGraphState(graph);
        GraphDiff diff = diffGraph(before, after);
        assertEquals(10, diff.added.size());
        assertEquals(0, diff.removed.size());

        List<String> names = done.stream()
                .flatMap(p -> StreamSupport
                        .stream(p.as(Described.class).getDescriptions().spliterator(), false)
                        .map(d -> d.<String>getProperty(Ontology.NAME_KEY)))
                .sorted()
                .collect(Collectors.toList());
        assertThat(names, equalTo(Lists.newArrayList("DANS Test",
                "KCL Test", "NIOD Test", "SOMA Test")));
        assertThat(api(validUser)
                .actionManager().getLatestGlobalEvent().getLogMessage(),
                        equalTo("This is a test"));
    }

    @Test
    public void testFindAndReplaceMaxItems() throws Exception {
        FindReplace findReplace = new FindReplace(graph, true, 2);
        List<VertexProxy> before = getGraphState(graph);
        List<Accessible> done = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.REPOSITORY_DESCRIPTION,
                "name", "Description",
                "Test", validUser, "This is a test");
        List<VertexProxy> after = getGraphState(graph);
        GraphDiff diff = diffGraph(before, after);
        assertEquals(6, diff.added.size());
        assertEquals(0, diff.removed.size());
        assertEquals(2, done.size());
    }

    @Test
    public void testFindAndReplaceDryRun() throws Exception {
        FindReplace findReplace = new FindReplace(graph, false, 100);
        List<VertexProxy> before = getGraphState(graph);
        List<Accessible> todo = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.REPOSITORY_DESCRIPTION,
                "name", "Description",
                "Test", validUser, "This is a test");
        List<VertexProxy> after = getGraphState(graph);
        GraphDiff diff = diffGraph(before, after);
        assertEquals(0, diff.added.size());
        assertEquals(0, diff.removed.size());
        assertEquals(4, todo.size());
    }

    @Test
    public void testFindAndReplaceNoneFound() throws Exception {
        FindReplace findReplace = new FindReplace(graph, false, 100);
        List<Accessible> todo = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.ADDRESS,
                "name", "Description",
                "Test", validUser, "This is a test");
        assertEquals(0, todo.size());
    }
}