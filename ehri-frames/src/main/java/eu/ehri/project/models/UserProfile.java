package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.*;

@EntityType(EntityClass.USER_PROFILE)
public interface UserProfile extends Accessor, AccessibleEntity, IdentifiableEntity,
        Annotator, Actioner, NamedEntity {

    @Fetch(Group.BELONGS_TO)
    @Adjacency(label = Group.BELONGS_TO)
    public Iterable<Group> getGroups();
}
