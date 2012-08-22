package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface TemporalEntity {

    public static final String HAS_DATE = "hasDate";

    @Adjacency(label = HAS_DATE)
    public abstract Iterable<DatePeriod> getDatePeriods();

    @Adjacency(label = HAS_DATE)
    public abstract void setDatePeriods(final Iterable<DatePeriod> datePeriods);

    @Adjacency(label = HAS_DATE)
    public abstract void addDatePeriod(final DatePeriod period);

}