package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface DatePeriod {
    public static final String isA = "datePeriod";
    
    @Property("startDate")
    public String getStartDate();
    
    @Property("endDate")
    public String getEndDate();
    
    @Adjacency(label=TemporalEntity.HAS_DATE, direction=Direction.IN)
    public TemporalEntity getEntity();
}
