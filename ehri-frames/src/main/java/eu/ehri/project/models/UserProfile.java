package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;

@EntityType(EntityClass.USER_PROFILE)
public interface UserProfile extends Accessor, AccessibleEntity, IdentifiableEntity,
        Annotator, Actioner, NamedEntity {

    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Group> getGroups();

    @Adjacency(label = Ontology.USER_FOLLOWS_USER, direction = Direction.OUT)
    public Iterable<UserProfile> getFollowing();

    @Adjacency(label = Ontology.USER_FOLLOWS_USER, direction = Direction.OUT)
    public void addFollowing(final UserProfile user);

    @Adjacency(label = Ontology.USER_FOLLOWS_USER, direction = Direction.OUT)
    public void removeFollowing(final UserProfile user);

    @JavaHandler
    public boolean isFollowing(final UserProfile otherUser);

    abstract class Impl implements JavaHandlerContext<Vertex>, UserProfile {
        public boolean isFollowing(final UserProfile otherUser) {
            return gremlin().out(Ontology.USER_FOLLOWS_USER).filter(new PipeFunction<Vertex, Boolean>() {
                @Override
                public Boolean compute(Vertex vertex) {
                    return vertex.equals(otherUser.asVertex());
                }
            }).hasNext();
        }
    }
}
