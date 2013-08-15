package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.TemporalEntity;

@EntityType(EntityClass.DATE_PERIOD)
public interface DatePeriod extends AnnotatableEntity {

    @Property(Ontology.DATE_PERIOD_START_DATE)
    public String getStartDate();

    @Property(Ontology.DATE_PERIOD_END_DATE)
    public String getEndDate();

    @Adjacency(label = Ontology.ENTITY_HAS_DATE, direction = Direction.IN)
    public TemporalEntity getEntity();
}
