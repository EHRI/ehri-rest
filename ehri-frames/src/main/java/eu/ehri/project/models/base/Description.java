package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

public interface Description extends VertexFrame {
    public static final String DESCRIBES = DescribedEntity.DESCRIBES;
    public static final String LANGUAGE_CODE = "languageCode";
    public static final String MUTATES = "maintenance"; //links to MaintenanceEvent
    public final static String NAME = "name";

    @Adjacency(label = DESCRIBES)
    public DescribedEntity getEntity();

    @Property(LANGUAGE_CODE)
    public String getLanguageOfDescription();
 
    @Property(NAME)
    public String getName();

    /**
     * Get the described entity of a description. This 
     * method if @Fetch serialized only if the description
     * is at the top level of the requested subtree.
     * 
     * @return
     */
    @Fetch(value = DESCRIBES, ifDepth=0)
    @Adjacency(label = DESCRIBES)
    public DescribedEntity getDescribedEntity();    
        
    @Dependent
    @Adjacency(label = MUTATES, direction=Direction.IN)
    public abstract Iterable<MaintenanceEvent> getMaintenanceEvents();

    @Adjacency(label = MUTATES)
    public abstract void setMaintenanceEvents(final Iterable<MaintenanceEvent> maintenanceEvents);

    @Adjacency(label = MUTATES)
    public abstract void addMaintenanceEvent(final MaintenanceEvent maintenanceEvent);
}
