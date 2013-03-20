package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotator;

@EntityType(EntityClass.USER_PROFILE)
public interface UserProfile extends Accessor, AccessibleEntity,
        Annotator, Actioner {

    @Fetch(Group.BELONGS_TO)
    @Adjacency(label = Group.BELONGS_TO)
    public Iterable<Group> getGroups();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);
}
