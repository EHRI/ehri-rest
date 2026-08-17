/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.definitions;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class IsadGTest {
    @Test
    public void testIsMultiValued() throws Exception {
        Assert.assertFalse(IsadG.scopeAndContent.isMultiValued());
    }

    @Test
    public void testName() throws Exception {
        Assert.assertEquals("scopeAndContent", IsadG.scopeAndContent.name());
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals("Scope and content", IsadG.scopeAndContent.getName());
    }

    @Test
    public void testGetDescription() throws Exception {
        Assert.assertThat(IsadG.scopeAndContent.getDescription(),
                CoreMatchers.containsString("Enables users to judge"));
    }

    @Test
    public void testGetMap() throws Exception {
        Assert.assertTrue(DefinitionList.getMap(IsadG.values())
                .keySet().contains("Scope and content"));
        Assert.assertTrue(DefinitionList.getMap(IsadG.values(), false)
                .keySet().contains("Scope and content"));
        Assert.assertFalse(DefinitionList.getMap(IsadG.values(), true)
                .keySet().contains("Scope and content"));
        Assert.assertTrue(DefinitionList.getMap(IsadG.values(), true)
                .keySet().contains("Notes"));
    }
}