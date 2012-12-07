package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityEnumType;

/**
 * Holds information about the collection or institute, 
 * but we don't specify (in the database) what it exactly means. 
 *
 */
@EntityEnumType(EntityEnumTypes.PROPERTY)
public interface Property extends VertexFrame { 
}
