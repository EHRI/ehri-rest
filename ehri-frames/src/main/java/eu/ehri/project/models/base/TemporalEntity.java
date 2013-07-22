package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

public interface TemporalEntity extends Frame {

    @Dependent
    @Fetch(value = Ontology.ENTITY_HAS_DATE, whenNotLite = true)
    @Adjacency(label = Ontology.ENTITY_HAS_DATE)
    public abstract Iterable<DatePeriod> getDatePeriods();

    @Adjacency(label = Ontology.ENTITY_HAS_DATE)
    public abstract void setDatePeriods(final Iterable<DatePeriod> datePeriods);

    @Adjacency(label = Ontology.ENTITY_HAS_DATE)
    public abstract void addDatePeriod(final DatePeriod period);

}