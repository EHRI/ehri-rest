package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;

public class AnnotationViewsTest extends AbstractFixtureTest {

    @Test
    public void testSubtreeAnnotationDump() throws ItemNotFound {
        AnnotationViews av = new AnnotationViews(graph);
        Iterable<Annotation> annotations = av.getFor("c1", validUser);
        // TODO: Test something
        assertFalse(Iterables.isEmpty(annotations));
    }
}
