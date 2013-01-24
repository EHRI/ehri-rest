package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ListMultimap;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.views.Annotations;
import eu.ehri.project.views.impl.AnnotationViews;

public class AnnotationViewsTest extends AbstractFixtureTest {

    @Test
    public void testSubtreeAnnotationDump() throws ItemNotFound {
        Annotations av = new AnnotationViews(graph);
        ListMultimap<String, Annotation> annotations = av.getFor("c1",
                validUser);
        // TODO: Test something
        assertFalse(annotations.isEmpty());
    }
}
