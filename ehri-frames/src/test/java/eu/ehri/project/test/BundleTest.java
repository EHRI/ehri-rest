package eu.ehri.project.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.AgentDescription;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Serializer;

public class BundleTest extends ModelTestBase {

    private static final String ID = "c1";

    private Serializer serializer;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        serializer = new Serializer(graph);
    }

    @Test
    public void testSerialisation() throws SerializationError,
            DeserializationError, ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        String json = serializer.vertexFrameToJson(c1);
        Bundle bundle = Bundle.fromString(json);
        assertEquals(ID, bundle.getId());

        // Test Agent serialization
        Agent r1 = manager.getFrame("r1", Agent.class);
        json = serializer.vertexFrameToJson(r1);
        bundle = Bundle.fromString(json);
        List<Bundle> descs = bundle.getRelations(DescribedEntity.DESCRIBES);
        assertEquals(1, descs.size());
        Bundle descBundle = descs.get(0);
        List<Bundle> addresses = descBundle
                .getRelations(AgentDescription.HAS_ADDRESS);
        assertEquals(1, addresses.size());
    }

    public void testSaving() throws SerializationError, ValidationError,
            IntegrityError, ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        assertEquals(1, toList(c1.getDescriptions()).size());

        Bundle bundle = serializer.vertexFrameToBundle(c1);
        BundleDAO persister = new BundleDAO(graph);
        DocumentaryUnit c1redux = persister.update(bundle,
                DocumentaryUnit.class);

        assertEquals(toList(c1.getDescriptions()),
                toList(c1redux.getDescriptions()));
    }

    public void testSavingAgent() throws SerializationError, ValidationError,
            IntegrityError, ItemNotFound {
        Agent r1 = manager.getFrame("r1", Agent.class);
        assertEquals(1, toList(r1.getDescriptions()).size());

        Bundle bundle = serializer.vertexFrameToBundle(r1);
        BundleDAO persister = new BundleDAO(graph);
        Agent r1redux = persister.update(bundle, Agent.class);

        assertEquals(toList(r1.getDescriptions()),
                toList(r1redux.getDescriptions()));

        AgentDescription ad1 = graph.frame(r1redux.getDescriptions().iterator()
                .next().asVertex(), AgentDescription.class);
        assertEquals(1, toList(ad1.getAddresses()).size());
    }

    @Test
    public void testSavingWithDependentChanges() throws SerializationError,
            DeserializationError, ValidationError, IntegrityError, ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        assertEquals(2, toList(c1.getDescriptions()).size());
        String json = serializer.vertexFrameToJson(c1);

        Description desc = toList(c1.getDescriptions()).get(0);
        c1.removeDescription(desc);
        assertEquals(1, toList(c1.getDescriptions()).size());

        // Restore the item from JSON
        Bundle bundle = Bundle.fromString(json);
        BundleDAO persister = new BundleDAO(graph);
        persister.update(bundle, DocumentaryUnit.class);

        // Our deleted description should have come back...
        assertEquals(2, toList(c1.getDescriptions()).size());
    }

    @Test
    public void testDeletingDependents() throws SerializationError,
            ValidationError, IntegrityError, ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        Bundle bundle = serializer.vertexFrameToBundle(c1);
        assertEquals(2, toList(c1.getDatePeriods()).size());
        String dpid = "c1-dp2";
        try {
            manager.getFrame(dpid, DatePeriod.class);
        } catch (ItemNotFound e) {
            fail("Date period '" + dpid
                    + "' not found in index before delete test.");
        }

        List<Bundle> dates = bundle.getRelations(TemporalEntity.HAS_DATE);
        Bundle newBundle = bundle.removeRelations(TemporalEntity.HAS_DATE)
                .withRelation(TemporalEntity.HAS_DATE, dates.get(0));
        BundleDAO persister = new BundleDAO(graph);
        persister.update(newBundle, DocumentaryUnit.class);
        assertEquals(1, toList(c1.getDatePeriods()).size());

        // The second date period should be gone from the index
        try {
            manager.getFrame(dpid, DatePeriod.class);
            fail("Date period '" + dpid + "' found in index AFTER delete test.");
        } catch (ItemNotFound e) {
        }

        // It should also not exist as a node...
        try {
            graph.getVertices(EntityType.ID_KEY, dpid).iterator().next();
            fail("Date period '" + dpid + "' found in index AFTER delete test.");
        } catch (NoSuchElementException e) {
        }
    }

    @Test(expected = ItemNotFound.class)
    public void testDeletingWholeBundle() throws SerializationError,
            ValidationError, ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        Bundle bundle = serializer.vertexFrameToBundle(c1);
        assertEquals(2, toList(c1.getDatePeriods()).size());
        List<DatePeriod> dates = toList(manager.getFrames(
                EntityClass.DATE_PERIOD, DatePeriod.class));

        BundleDAO persister = new BundleDAO(graph);
        Integer numDeleted = persister.delete(bundle);
        assertTrue(numDeleted > 0);
        assertEquals(
                dates.size() - 2,
                toList(
                        manager.getFrames(EntityClass.DATE_PERIOD,
                                DatePeriod.class)).size());
        // Should raise NoSuchElementException
        manager.getFrame(ID, DocumentaryUnit.class);
    }

    @Test(expected = ValidationError.class)
    public void testValidationError() throws SerializationError,
            ValidationError, ItemNotFound, IntegrityError {
        DocumentaryUnit c1 = manager.getFrame(ID, DocumentaryUnit.class);
        Bundle bundle = serializer.vertexFrameToBundle(c1);

        List<Bundle> dates = bundle.getRelations(TemporalEntity.HAS_DATE);
        // remove the start date key from a date
        Bundle invalidDate = dates.get(0).withData(
                Maps.<String, Object> newHashMap());
        Bundle newBundle = bundle.withRelation(TemporalEntity.HAS_DATE,
                invalidDate);

        BundleDAO persister = new BundleDAO(graph);
        persister.update(newBundle, DocumentaryUnit.class);
        fail("Bundle with invalid dates did not throw a ValidationError");
    }
}
