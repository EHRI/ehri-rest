package eu.ehri.project.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class FindReplaceTest extends AbstractFixtureTest {

    @Test
    public void testFindAndReplace() throws Exception {
        FindReplace findReplace = new FindReplace(graph, true, 100);
        List<VertexProxy> before = getGraphState(graph);
        List<List<String>> names = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.REPOSITORY_DESCRIPTION,
                "name", "Description",
                "Test", validUser, "This is a test");
        names.sort(Comparator.comparing(o -> o.get(0)));
        List<VertexProxy> after = getGraphState(graph);
        GraphDiff diff = diffGraph(before, after);
        assertEquals(10, diff.added.size());
        assertEquals(0, diff.removed.size());

        List<List<String>> out = ImmutableList.of(
                Lists.newArrayList("r1", "rd1", "NIOD Description"),
                Lists.newArrayList("r2", "rd2", "KCL Description"),
                Lists.newArrayList("r3", "rd3", "DANS Description"),
                Lists.newArrayList("r4", "rd4", "SOMA Description")
        );

        assertEquals("NIOD Test",
                manager.getEntity("rd1", Description.class).getName());
        assertEquals("KCL Test",
                manager.getEntity("rd2", Description.class).getName());
        assertEquals("DANS Test",
                manager.getEntity("rd3", Description.class).getName());
        assertEquals("SOMA Test",
                manager.getEntity("rd4", Description.class).getName());

        assertThat(names, equalTo(out));
        assertThat(api(validUser)
                        .actionManager().getLatestGlobalEvent().getLogMessage(),
                equalTo("This is a test"));
    }

    @Test
    public void testFindAndReplaceMaxItems() throws Exception {
        FindReplace findReplace = new FindReplace(graph, true, 2);
        List<VertexProxy> before = getGraphState(graph);
        List<List<String>> done = findReplace.findAndReplace(
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
        List<List<String>> todo = findReplace.findAndReplace(
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
        List<List<String>> todo = findReplace.findAndReplace(
                EntityClass.REPOSITORY, EntityClass.ADDRESS,
                "name", "Description",
                "Test", validUser, "This is a test");
        assertEquals(0, todo.size());
    }
}