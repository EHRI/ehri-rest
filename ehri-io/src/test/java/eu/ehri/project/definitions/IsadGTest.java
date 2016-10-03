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