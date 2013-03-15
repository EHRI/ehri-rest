package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Description;

@EntityType(EntityClass.AUTHORITY_DESCRIPTION)
public interface AuthorityDescription extends VertexFrame, Description, AddressableEntity {
    

    
}
