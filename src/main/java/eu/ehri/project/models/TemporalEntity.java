package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface TemporalEntity {

    @Adjacency(label = "hasDate")
    public abstract Iterable<DatePeriod> getDatePeriods();

    @Adjacency(label = "hasDate")
    public abstract void setDatePeriods(final Iterable<DatePeriod> datePeriods);

    @Adjacency(label = "hasDate")
    public abstract void addDatePeriod(final DatePeriod period);

}