package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;

public interface IdentifiableEntity extends Frame {

    public static final String IDENTIFIER_KEY = "identifier";

    @Mandatory
    @Property(IDENTIFIER_KEY)
    public String getIdentifier();
}
