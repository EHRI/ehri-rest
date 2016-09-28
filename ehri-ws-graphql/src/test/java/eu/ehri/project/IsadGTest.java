package eu.ehri.project.definitions;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class IsadGTest {
    @Test
    public void testIsMultiValued() throws Exception {
        assertFalse(IsadG.scopeAndContent.isMultiValued());
    }

    @Test
    public void testName() throws Exception {
        assertEquals("scopeAndContent", IsadG.scopeAndContent.name());
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals("Scope and content", IsadG.scopeAndContent.getName());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertThat(IsadG.scopeAndContent.getDescription(),
                containsString("Enables users to judge"));
    }

    @Test
    public void testGetMap() throws Exception {
        assertTrue(DefinitionList.getMap(IsadG.values())
                .keySet().contains("Scope and content"));
        assertTrue(DefinitionList.getMap(IsadG.values(), false)
                .keySet().contains("Scope and content"));
        assertFalse(DefinitionList.getMap(IsadG.values(), true)
                .keySet().contains("Scope and content"));
        assertTrue(DefinitionList.getMap(IsadG.values(), true)
                .keySet().contains("Notes"));
    }
}