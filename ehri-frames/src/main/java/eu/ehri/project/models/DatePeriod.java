package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.TemporalEntity;

/**
 * Frame class representing a date period.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.DATE_PERIOD)
public interface DatePeriod extends AnnotatableEntity {

    /**
     * The start date in UTC format.
     *
     * @return A UTC date string
     */
    @Property(Ontology.DATE_PERIOD_START_DATE)
    public String getStartDate();

    /**
     * The end date in UTC format.
     *
     * @return A UTC string
     */
    @Property(Ontology.DATE_PERIOD_END_DATE)
    public String getEndDate();

    /**
     * Get the entity described by this date period.
     *
     * @return a temporal item
     */
    @Adjacency(label = Ontology.ENTITY_HAS_DATE, direction = Direction.IN)
    public TemporalEntity getEntity();
}
