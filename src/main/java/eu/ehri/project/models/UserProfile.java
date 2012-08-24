package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Annotator;

@EntityType(EntityTypes.USER_PROFILE)
public interface UserProfile extends Accessor, Annotator {

    public static final String BELONGS_TO = "belongsTo";

    @Adjacency(label = BELONGS_TO)
    public Iterable<Group> getGroups();

    @Property("userId")
    public Long getUserId();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Property("about")
    public String getAbout();

    @Property("about")
    public void setAbout(String name);

    @Property("languages")
    public String[] getLanguages();

    @Property("languages")
    public void setLanguages(String[] languages);

    @Property("location")
    public String getLocation();

    @Property("location")
    public void setLocation(String location);
}
