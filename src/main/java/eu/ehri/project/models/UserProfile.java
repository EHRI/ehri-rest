package eu.ehri.project.models;

import com.tinkerpop.frames.*;

public interface UserProfile extends Accessor {

    @Adjacency(label = "hasAnnotations")
    public Iterable<Annotation> getAnnotation();

    @Adjacency(label = "belongsTo")
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
