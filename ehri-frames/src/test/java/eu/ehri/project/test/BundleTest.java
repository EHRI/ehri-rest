package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;

public class BundleTest extends ModelTestBase {

    private Converter converter;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        converter = new Converter();
    }

    @Test
    public void testSerialisation() throws SerializationError,
            DeserializationError {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        String json = converter.vertexFrameToJson(c1);
        EntityBundle<DocumentaryUnit> bundle = converter.jsonToBundle(json);
        // FIXME: Comparing ids fails because the deserialized on is an <int>
        // instead
        // of a long...
        assertEquals(c1.getName(), bundle.getData().get("name"));
    }

    public void testSaving() throws SerializationError, ValidationError, IntegrityError {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        assertEquals(1, toList(c1.getDescriptions()).size());

        EntityBundle<DocumentaryUnit> bundle = converter
                .vertexFrameToBundle(c1);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                graph);
        DocumentaryUnit c1redux = persister.update(bundle);

        assertEquals(toList(c1.getDescriptions()),
                toList(c1redux.getDescriptions()));
    }

    @Test
    public void testSavingWithDependentChanges() throws SerializationError,
            DeserializationError, ValidationError, IntegrityError {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        assertEquals(1, toList(c1.getDescriptions()).size());
        String json = converter.vertexFrameToJson(c1);

        Description desc = toList(c1.getDescriptions()).get(0);
        c1.removeDescription(desc);
        assertEquals(0, toList(c1.getDescriptions()).size());

        // Restore the item from JSON
        EntityBundle<DocumentaryUnit> bundle = converter.jsonToBundle(json);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                graph);
        persister.update(bundle);

        // Our deleted description should have come back...
        assertEquals(1, toList(c1.getDescriptions()).size());
    }

    @Test
    public void testDeletingDependents() throws SerializationError,
            ValidationError, IntegrityError {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        EntityBundle<DocumentaryUnit> bundle = converter
                .vertexFrameToBundle(c1);
        assertEquals(2, toList(c1.getDatePeriods()).size());

        Collection<EntityBundle<DatePeriod>> dates = bundle.getRelations()
                .getCollection(TemporalEntity.HAS_DATE);

        // Dodginess! Manipulate the bundles relations directly
        // by replacing the hasDate collection with one consisting
        // of only one date.
        bundle.getRelations().remove(TemporalEntity.HAS_DATE);
        Collection<EntityBundle<DatePeriod>> newDates = new ArrayList<EntityBundle<DatePeriod>>();
        newDates.add(dates.iterator().next());
        bundle.getRelations().putAll(TemporalEntity.HAS_DATE, newDates);

        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                graph);
        persister.update(bundle);
        assertEquals(1, toList(c1.getDatePeriods()).size());
    }

    @Test(expected = NoSuchElementException.class)
    public void testDeletingWholeBundle() throws SerializationError,
            ValidationError {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        EntityBundle<DocumentaryUnit> bundle = converter
                .vertexFrameToBundle(c1);
        assertEquals(2, toList(c1.getDatePeriods()).size());
        List<DatePeriod> dates = toList(manager.getFrames(
                EntityTypes.DATE_PERIOD, DatePeriod.class));

        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(
                graph);
        Integer numDeleted = persister.delete(bundle);
        assertTrue(numDeleted > 0);
        assertEquals(
                dates.size() - 2,
                toList(
                        manager.getFrames(EntityTypes.DATE_PERIOD,
                                DatePeriod.class)).size());
        // Should raise NoSuchElementException
        manager.getFrame("c1", DocumentaryUnit.class);
    }
}
