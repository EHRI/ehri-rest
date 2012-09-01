package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.relationships.Annotates;

public class AnnotationTest extends ModelTestBase {

    // NB: These must match up with the JSON fixture...
    public static final String TEST_ANNOTATION_BODY = "Test Annotation";
    public static final String TEST_ANNOTATION_ANNOTATION_BODY = "Test Annotation of Annotation";
    private static final String TEST_ANNOTATION_CONTEXT_FIELD = "scopeAndContent";

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testUserHasAnnotation() {
        UserProfile mike = helper.getTestFrame("mike", UserProfile.class);
        assertTrue(mike.getAnnotations().iterator().hasNext());
        assertEquals(mike.getAnnotations().iterator().next().getBody(),
                TEST_ANNOTATION_BODY);
    }

    @Test
    public void testUserHasAnnotationWithTarget() {
        UserProfile mike = helper.getTestFrame("mike", UserProfile.class);
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        Annotation annotation = mike.getAnnotations().iterator().next();
        Annotates context = annotation.getContext().iterator().next();
        assertFalse(context == null);
        assertEquals(toList(mike.getAnnotations()).get(0), annotation);
        assertEquals(toList(c1.getAnnotations()).get(0), annotation);

        // Check target
        assertEquals(annotation.getTarget().asVertex().getId(), c1.asVertex()
                .getId());
        // And the context field
        assertEquals(context.getField(), TEST_ANNOTATION_CONTEXT_FIELD);
    }

    @Test
    public void testAnnotationAnnotation() {
        AnnotatableEntity ann1 = helper.getTestFrame("ann1",
                AnnotatableEntity.class);
        Annotation ann2 = helper.getTestFrame("ann2", Annotation.class);

        assertEquals(ann2.getTarget(), ann1);
        assertEquals(ann1.getAnnotations().iterator().next().getBody(),
                ann2.getBody());
    }
}
