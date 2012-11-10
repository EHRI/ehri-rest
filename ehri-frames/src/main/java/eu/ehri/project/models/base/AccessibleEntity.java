package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Action;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Unique;

public interface AccessibleEntity extends VertexFrame, PermissionGrantTarget {

    public static final String ACCESS = "access";
    public static final String IDENTIFIER_KEY = "identifier";

    @Unique
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();

    @Fetch(depth=1)
    @Adjacency(label = ACCESS)
    public Iterable<Accessor> getAccessors();

    @Adjacency(label = ACCESS)
    public void addAccessor(final Accessor accessor);

    @Adjacency(label = ACCESS)
    public void removeAccessor(final Accessor accessor);

    @Adjacency(label = PermissionGrant.HAS_ENTITY)
    public Iterable<PermissionGrant> getPermissionAssertions();

    @Adjacency(label = Action.HAS_SUBJECT, direction = Direction.IN)
    public Iterable<Action> getHistory();
}
