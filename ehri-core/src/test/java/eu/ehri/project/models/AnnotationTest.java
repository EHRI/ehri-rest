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

package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotationTest extends ModelTestBase {

    // FIXME: These tests depend on iteration order, which is not guaranteed!

    // NB: These must match up with the JSON fixture...
    public static final String TEST_ANNOTATION_BODY = "Test Annotation";
    public static final String TEST_ANNOTATION_ANNOTATION_BODY = "Test Annotation of Annotation";

    @Test
    public void testUserHasAnnotation() throws ItemNotFound {
        UserProfile mike = manager.getEntity("mike", UserProfile.class);
        assertTrue(mike.getAnnotations().iterator().hasNext());
        assertEquals(mike.getAnnotations().iterator().next().getBody(),
                TEST_ANNOTATION_BODY);
    }

    @Test
    public void testUserHasAnnotationWithTarget() throws ItemNotFound {
        UserProfile mike = manager.getEntity("mike", UserProfile.class);
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        Annotation annotation = mike.getAnnotations().iterator().next();
        assertTrue(Iterables.contains(mike.getAnnotations(), annotation));
        assertTrue(Iterables.contains(c1.getAnnotations(), annotation));
    }

    @Test
    public void testAnnotationAnnotation() throws ItemNotFound {
        Annotatable ann1 = manager.getEntity("ann1", Annotatable.class);
        Annotation ann2 = manager.getEntity("ann2", Annotation.class);
        assertEquals(ann2.getTargets().iterator().next(), ann1);
        assertEquals(TEST_ANNOTATION_ANNOTATION_BODY, ann2.getBody());
        assertEquals(ann1.getAnnotations().iterator().next().getBody(),
                ann2.getBody());
    }

    @Test
    public void testGetAnnotator() throws Exception {
        UserProfile mike = manager.getEntity("mike", UserProfile.class);
        Annotation ann1 = manager.getEntity("ann1", Annotation.class);
        assertEquals(mike, ann1.getAnnotator());
    }

    @Test
    public void testGetAnnotationTargetPart() throws Exception {
        Annotation ann1 = manager.getEntity("ann1", Annotation.class);
        List<Annotatable> parts = Lists.newArrayList(ann1.getTargetParts());
        assertEquals(1, parts.size());
        assertEquals(manager.getEntity("cd1", Description.class), parts.get(0));
    }
}
