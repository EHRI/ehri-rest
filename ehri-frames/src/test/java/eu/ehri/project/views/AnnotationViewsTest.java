/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.views;

import com.google.common.collect.Lists;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
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
    public Group canAnnotate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        acl = new AclManager(graph);
        av = new AnnotationViews(graph);
        admin = manager.getFrame("mike", UserProfile.class);
        user = manager.getFrame("reto", UserProfile.class);
        canAnnotate = manager.getFrame("portal", Group.class);
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
        canAnnotate.addMember(user);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c1", "cd1", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateForOutsideSubtree() throws Exception {
        canAnnotate.addMember(user);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c4", "cd1", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }

    @Test
    public void testCreateFor() throws Exception {
        canAnnotate.addMember(user);
        Bundle ann = new Bundle(EntityClass.ANNOTATION)
                .withDataValue(Ontology.ANNOTATION_NOTES_BODY, "test");
        Annotation annotation = av
                .createFor("c4", "cd4", ann, user, Lists.<Accessor>newArrayList());
        assertEquals("test", annotation.getBody());
    }
}
