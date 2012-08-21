package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface Collection extends Entity {

    @Adjacency(label = "holds", direction = Direction.IN)
    public CHInstitution getCHInstitution();

    @Adjacency(label = "holds", direction = Direction.IN)
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
