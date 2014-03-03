package eu.ehri.project.views;

import com.google.common.collect.Lists;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnnotationViewsTest extends AbstractFixtureTest {

    public AnnotationViews av;
    public AclManager acl;
    public UserProfile admin;
    public UserProfile user;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        acl = new AclManager(graph);
        av = new AnnotationViews(graph);
        admin = manager.getFrame("mike", UserProfile.class);
        user = manager.getFrame("reto", UserProfile.class);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateForWithoutPermission() throws Exception {
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c4", "cd4", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }

    @Test(expected = AccessDenied.class)
    public void testCreateForWithoutAccess() throws Exception {
        acl.grantPermissions(user, manager.getFrame(
                EntityClass.DOCUMENTARY_UNIT.getName(), ContentType.class),
                PermissionType.ANNOTATE);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c1", "cd1", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateForOutsideSubtree() throws Exception {
        acl.grantPermissions(user, manager.getFrame(
                EntityClass.DOCUMENTARY_UNIT.getName(), ContentType.class),
                PermissionType.ANNOTATE);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c4", "cd1", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }

    @Test
    public void testCreateFor() throws Exception {
        acl.grantPermissions(user, manager.getFrame(
                EntityClass.DOCUMENTARY_UNIT.getName(), ContentType.class),
                PermissionType.ANNOTATE);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c4", "cd4", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }
}
