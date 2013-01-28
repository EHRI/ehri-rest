package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

public interface TemporalEntity extends VertexFrame {

    public static final String HAS_DATE = "hasDate";

    @Dependent
    @Fetch(HAS_DATE)
    @Adjacency(label = HAS_DATE)
    public abstract Iterable<DatePeriod> getDatePeriods();

    @Adjacency(label = HAS_DATE)
    public abstract void setDatePeriods(final Iterable<DatePeriod> datePeriods);

    @Adjacency(label = HAS_DATE)
    public abstract void addDatePeriod(final DatePeriod period);

}