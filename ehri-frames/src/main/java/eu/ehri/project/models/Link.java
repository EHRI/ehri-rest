/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.LinkableEntity;

/**
 *
 * Links two items together with a given body, with may either be
 * a text property or some other entity.
 * 
 * @author mik
 */
@EntityType(EntityClass.LINK)
public interface Link extends AccessibleEntity, AnnotatableEntity {

    public static final String HAS_LINK_BODY = "hasLinkBody";
    public static final String HAS_LINK_TARGET = "hasLinkTarget";
    public static final String HAS_LINKER = "hasLinker";
    public static final String LINK_TYPE = "type";
    public static final String LINK_DESCRIPTION = "description";

    @Fetch(HAS_LINKER)
    @Adjacency(label = HAS_LINKER)
    public Iterable<UserProfile> getLinker();

    @Adjacency(label = HAS_LINKER)
    public void setLinker(final Accessor accessor);

    @Fetch(ifDepth = 0, value = HAS_LINK_TARGET)
    @Adjacency(label = HAS_LINK_TARGET)
    public Iterable<LinkableEntity> getLinkTargets();

    @Adjacency(label = HAS_LINK_TARGET)
    public void addLinkTarget(final LinkableEntity entity);

    @Fetch(HAS_LINK_BODY)
    @Adjacency(label = HAS_LINK_BODY)
    public Iterable<AccessibleEntity> getLinkBodies();

    @Adjacency(label = HAS_LINK_BODY)
    public void addLinkBody(final AccessibleEntity entity);

    @Mandatory
    @Property(LINK_TYPE)
    public String getLinkType();

    @Property(LINK_DESCRIPTION)
    public String getDescription();
}




