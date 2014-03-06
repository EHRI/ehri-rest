package eu.ehri.project.views;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LinkViewsTest extends AbstractFixtureTest {

    private LinkViews linkViews;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        linkViews = new LinkViews(graph);
    }

    @Test
    public void testCreateLink() throws Exception {
        DocumentaryUnit src = manager.getFrame("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getFrame("a1", HistoricalAgent.class);
        UndeterminedRelationship rel = manager.getFrame("ur1", UndeterminedRelationship.class);
        String linkDesc = "Test Link";
        String linkType = "subjectAccess";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        Link link = linkViews.createLink("c1", "a1", Lists.newArrayList("ur1"),
                linkBundle, validUser);
        assertEquals(linkDesc, link.getDescription());
        assertEquals(2L, Iterables.size(link.getLinkTargets()));
        assertTrue(Iterables.contains(link.getLinkTargets(), src));
        assertTrue(Iterables.contains(link.getLinkTargets(), dst));
        assertEquals(1L, Iterables.size(link.getLinkBodies()));
        assertTrue(Iterables.contains(link.getLinkBodies(), rel));
    }


    @Test(expected = PermissionDenied.class)
    public void testCreateLinkWithoutPermission() throws Exception {
        linkViews.createLink("c1", "a1", Lists.newArrayList("ur1"),
                getLinkBundle("won't work!", "too bad!"), invalidUser);
    }

    @Test
    public void testCreateAccessPointLink() throws Exception {
        DocumentaryUnit src = manager.getFrame("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getFrame("a1", HistoricalAgent.class);
        DocumentDescription desc = manager.getFrame("cd1", DocumentDescription.class);
        String linkDesc = "Test Link";
        String linkType = "subjectAccess";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        Link link = linkViews.createAccessPointLink("c1", "a1", "cd1", linkDesc, linkType,
                linkBundle, validUser);
        assertEquals(linkDesc, link.getDescription());
        assertEquals(2L, Iterables.size(link.getLinkTargets()));
        assertTrue(Iterables.contains(link.getLinkTargets(), src));
        assertTrue(Iterables.contains(link.getLinkTargets(), dst));
        assertEquals(1L, Iterables.size(link.getLinkBodies()));
        UndeterminedRelationship rel = manager.cast(link.getLinkBodies().iterator().next(),
                UndeterminedRelationship.class);
        assertEquals(rel.getName(), linkDesc);
        assertEquals(rel.getRelationshipType(), linkType);
        Description d = rel.getDescription();
        assertEquals(desc, d);
    }

    private Bundle getLinkBundle(String linkDesc, String linkType) {
        return new Bundle.Builder(EntityClass.LINK)
                .addDataValue(Ontology.LINK_HAS_TYPE, linkType)
                .addDataValue(Ontology.LINK_HAS_DESCRIPTION, linkDesc)
                .build();
    }
}