package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityTypes.DATE_PERIOD)
public interface DatePeriod {
    
    @Property("startDate")
    public String getStartDate();
    
    @Property("endDate")
    public String getEndDate();
    
    @Adjacency(label=TemporalEntity.HAS_DATE, direction=Direction.IN)
    public TemporalEntity getEntity();
}
