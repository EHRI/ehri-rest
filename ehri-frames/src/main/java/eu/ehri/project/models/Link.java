package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.LinkableEntity;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Links two items together with a given body, with may either be
 * a text property or some other entity.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.LINK)
public interface Link extends AccessibleEntity, AnnotatableEntity, Promotable, TemporalEntity {

    @Fetch(Ontology.LINK_HAS_LINKER)
    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    public UserProfile getLinker();

    @Adjacency(label = Ontology.LINK_HAS_LINKER)
    public void setLinker(final Accessor accessor);

    @Fetch(ifDepth = 0, value = Ontology.LINK_HAS_TARGET)
    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    public Iterable<LinkableEntity> getLinkTargets();

    @Adjacency(label = Ontology.LINK_HAS_TARGET)
    public void addLinkTarget(final LinkableEntity entity);

    @Fetch(Ontology.LINK_HAS_BODY)
    @Adjacency(label = Ontology.LINK_HAS_BODY)
    public Iterable<AccessibleEntity> getLinkBodies();

    @Adjacency(label = Ontology.LINK_HAS_BODY)
    public void addLinkBody(final AccessibleEntity entity);

    @Mandatory
    @Property(Ontology.LINK_HAS_TYPE)
    public String getLinkType();

    @Property(Ontology.LINK_HAS_DESCRIPTION)
    public String getDescription();
}




