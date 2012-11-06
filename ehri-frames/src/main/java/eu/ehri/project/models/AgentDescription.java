package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityTypes.AGENT_DESCRIPTION)
public interface AgentDescription extends Description {

    @Property("name")
    public String getTitle();

    @Property("otherFormsOfName")
    public String[] otherFormsOfName();

    @Property("parallelFormsOfName")
    public String[] parallelFormsOfName();
}
