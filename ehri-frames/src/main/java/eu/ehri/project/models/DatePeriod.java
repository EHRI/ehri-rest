package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.DATE_PERIOD)
public interface DatePeriod extends AnnotatableEntity {

    static final String START_DATE = "startDate";
    static final String END_DATE = "endDate";

    @Property(START_DATE)
    public String getStartDate();

    @Property(END_DATE)
    public String getEndDate();

    @Adjacency(label = TemporalEntity.HAS_DATE, direction = Direction.IN)
    public TemporalEntity getEntity();
}
