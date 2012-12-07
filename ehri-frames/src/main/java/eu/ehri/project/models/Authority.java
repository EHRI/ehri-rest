package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;

@EntityEnumType(EntityEnumTypes.AUTHORITY)
public interface Authority extends VertexFrame, AccessibleEntity,
        DescribedEntity, AnnotatableEntity {

    public static final String CREATED = "created";
    public static final String MENTIONED_IN = "mentionedIn";

    @Property("typeOfEntity")
    public String getTypeOfEntity();

    @Adjacency(label = CREATED)
    public Iterable<DocumentaryUnit> getDocumentaryUnits();

    @Adjacency(label = CREATED)
    public void addDocumentaryUnit(final DocumentaryUnit unit);

    @Adjacency(label = MENTIONED_IN)
    public Iterable<DocumentaryUnit> getMentionedIn();

    @Adjacency(label = MENTIONED_IN)
    public void addMentionedIn(final DocumentaryUnit unit);
    
    @Fetch
    @Dependent
    @Adjacency(label = DESCRIBES, direction = Direction.IN)
    public Iterable<AuthorityDescription> getDescriptions();        
}
