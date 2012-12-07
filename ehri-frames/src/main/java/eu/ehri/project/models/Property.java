package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.annotations.EntityType;

/**
 * Holds information about the collection or institute, 
 * but we don't specify (in the database) what it exactly means. 
 *
 */
@EntityType(EntityTypes.PROPERTY)
@EntityEnumType(EntityEnumTypes.PROPERTY)
public interface Property extends VertexFrame { 
}
