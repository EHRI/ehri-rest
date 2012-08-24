package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;

@EntityType(EntityTypes.AUTHORITY)
public interface Authority extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {
    
    public static final String CREATED = "created";
    public static final String MENTIONED_IN = "mentionedIn";
    
    @Property("type_of_entity")
    public String getTypeOfEntity();
    
    @Adjacency(label = CREATED)
    public Iterable<DocumentaryUnit> getDocumentaryUnits();
    
    @Adjacency(label = CREATED)
    public void addDocumentaryUnit(final DocumentaryUnit unit);
    
    @Adjacency(label = MENTIONED_IN)
    public Iterable<DocumentaryUnit> getMentionedIn();    

    @Adjacency(label = MENTIONED_IN)
    public void addMentionedIn(final DocumentaryUnit unit);
}
