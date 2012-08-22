package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface UserProfile extends Accessor {

    public static final String isA = "userProfile";
    public static final String HAS_ANNOTATION = "hasAnnotation";
    public static final String BELONGS_TO = "belongsTo";

    @Adjacency(label = HAS_ANNOTATION)
    public Iterable<Annotation> getAnnotation();

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
