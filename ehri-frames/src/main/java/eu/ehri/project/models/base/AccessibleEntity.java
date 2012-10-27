package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Action;
import eu.ehri.project.models.PermissionAssertion;
import eu.ehri.project.models.annotations.Unique;
import eu.ehri.project.relationships.Access;

public interface AccessibleEntity extends VertexFrame {

    public static final String ACCESS = "access";
    public static final String IDENTIFIER_KEY = "identifier";

    @Unique
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();

    @Incidence(label = ACCESS)
    public Iterable<Access> getAccess();

    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessibleTo();

    @Adjacency(label = ACCESS)
    public void removeAccessor(final Accessor accessor);
    
    @Adjacency(label = PermissionAssertion.HAS_ENTITY)
    public Iterable<PermissionAssertion> getPermissionAssertions();    

    @Adjacency(label = Action.HAS_SUBJECT, direction = Direction.IN)
    public Iterable<Action> getHistory();
}
