package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;

@EntityEnumType(EntityEnumTypes.ACTION)
public interface Action extends AccessibleEntity {
    public static final String HAS_SUBJECT = "hasSubject";
    public static final String HAS_ACTIONER = "hasActioner";
    
    public final String TIMESTAMP = "timestamp";
    public final String LOG_MESSAGE = "logMessage";

    @Property(TIMESTAMP)
    public String getTimestamp();

    @Property(TIMESTAMP)
    public void setTimestamp(String timestamp);

    @Property(LOG_MESSAGE)
    public String getLogMessage();

    @Property(LOG_MESSAGE)
    public void setLogMessage(String message);

    @Adjacency(label = HAS_SUBJECT)
    public Iterable<AccessibleEntity> getSubjects();

    @Adjacency(label = HAS_SUBJECT)
    public void addSubjects(final AccessibleEntity subject);

    @Adjacency(label = HAS_SUBJECT)
    public void setSubject(final AccessibleEntity subject);

    @Fetch
    @Adjacency(label = HAS_ACTIONER)
    public UserProfile getActioner();

    @Adjacency(label = HAS_ACTIONER)
    public void setActioner(final Actioner user);
}
