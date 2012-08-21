package eu.ehri.project.models;

import com.tinkerpop.frames.*;

public interface Collection extends Entity {

    @Adjacency(label = "holds")
    public CHInstitution getCHInstitution();

    @Adjacency(label = "holds")
    public void setCHInstitution(final CHInstitution institution);

    @Adjacency(label = "hasDate")
    public Iterable<DatePeriod> getDatePeriods();

    @Adjacency(label = "hasDate")
    public void setDatePeriods(final Iterable<DatePeriod> datePeriods);

    @Adjacency(label = "hasDate")
    public void addDatePeriod(final DatePeriod period);

    @Adjacency(label = "describes")
    public Iterable<CollectionDescription> getDescriptions();

    @Adjacency(label = "describes")
    public void addDescription(final CollectionDescription description);
}
