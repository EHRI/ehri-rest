package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityClass.AUTHORITY_DESCRIPTION)
public interface AuthorityDescription extends VertexFrame, Description {

    public static final String AUTHORITY_NAME = "name"; // Authorized form of name
    public static final String AUTHORITY_TYPE = "typeOfEntity";

    @Property(AUTHORITY_NAME)
    public String getName();

    @Property(AUTHORITY_TYPE)
    public String getTypeOfEntity();

}
