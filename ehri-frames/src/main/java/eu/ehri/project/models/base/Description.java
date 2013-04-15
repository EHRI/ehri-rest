package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;

public interface Description extends NamedEntity {
    public static final String DESCRIBES = DescribedEntity.DESCRIBES;
    public static final String LANGUAGE_CODE = "languageCode";
    public static final String MUTATES = "maintenance"; //links to MaintenanceEvent
    public static final String RELATES_TO = "relatesTo"; //links to UndeterminedRelationship

    @Adjacency(label = DESCRIBES)
    public DescribedEntity getEntity();

    @Mandatory
    @Property(LANGUAGE_CODE)
    public String getLanguageOfDescription();

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
    
    @Dependent
    @Adjacency(label = RELATES_TO, direction=Direction.IN)
    public abstract Iterable<UndeterminedRelationship> getUndeterminedRelationships();

    @Adjacency(label = RELATES_TO)
    public abstract void setUndeterminedRelationships(final Iterable<UndeterminedRelationship> relationship);

    @Adjacency(label = RELATES_TO)
    public abstract void addUndeterminedRelationship(final UndeterminedRelationship relationship);
}
