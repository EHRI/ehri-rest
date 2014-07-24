package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;

public interface Description extends NamedEntity, AccessibleEntity {

    /**
     * Process by which this description was created. Currently supported
     * values allow for automatic import or manual creation (by a human).
     */
    public static enum CreationProcess {
        MANUAL, IMPORT
    }

    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY)
    public DescribedEntity getEntity();

    @Mandatory
    @Property(Ontology.LANGUAGE_OF_DESCRIPTION)
    public String getLanguageOfDescription();

    @Property(Ontology.CREATION_PROCESS)
    public CreationProcess getCreationProcess();

    /**
     * Get the described entity of a description. This 
     * method if @Fetch serialized only if the description
     * is at the top level of the requested subtree.
     * 
     * @return
     */
    @Fetch(value = Ontology.DESCRIPTION_FOR_ENTITY, ifDepth=0)
    @Adjacency(label = Ontology.DESCRIPTION_FOR_ENTITY)
    public DescribedEntity getDescribedEntity();    

    @Dependent
    @Fetch(value = Ontology.HAS_MAINTENANCE_EVENT, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    public abstract Iterable<MaintenanceEvent> getMaintenanceEvents();

    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    public abstract void setMaintenanceEvents(final Iterable<MaintenanceEvent> maintenanceEvents);

    @Adjacency(label = Ontology.HAS_MAINTENANCE_EVENT, direction=Direction.IN)
    public abstract void addMaintenanceEvent(final MaintenanceEvent maintenanceEvent);

    @Dependent
    @Fetch(value = Ontology.HAS_ACCESS_POINT, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_ACCESS_POINT)
    public Iterable<UndeterminedRelationship> getUndeterminedRelationships();

    @Adjacency(label = Ontology.HAS_ACCESS_POINT)
    public void setUndeterminedRelationships(final Iterable<UndeterminedRelationship> relationship);

    @Adjacency(label = Ontology.HAS_ACCESS_POINT)
    public void addUndeterminedRelationship(final UndeterminedRelationship relationship);

    @Dependent
    @Fetch(value = Ontology.HAS_UNKNOWN_PROPERTY, ifDepth = 1, whenNotLite = true)
    @Adjacency(label = Ontology.HAS_UNKNOWN_PROPERTY)
    public Iterable<UnknownProperty> getUnknownProperties();
}
