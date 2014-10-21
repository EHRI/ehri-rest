package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * A frame class representing a item that holds other
 * <i>authoritative</i> items, such as concepts and
 * historical agents.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
@EntityType(EntityClass.AUTHORITATIVE_SET)
public interface AuthoritativeSet extends AccessibleEntity, IdentifiableEntity,
        PermissionScope, ItemHolder, NamedEntity {

    /**
     * Fetch all items within this set.
     *
     * @return an iterable of authoritative items
     */
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    public Iterable<AuthoritativeItem> getAuthoritativeItems();

    /**
     * Add an authoritative item to this set.
     *
     * @param item an authoritative item frame
     */
    @JavaHandler
    public void addItem(final AuthoritativeItem item);

    /**
     * Update the cache for the number of items within
     * this set.
     */
    @JavaHandler
    public void updateChildCountCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, AuthoritativeSet {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET).count());
        }

        public long getChildCount() {
            Long count = it().getProperty(CHILD_COUNT);
            if (count == null) {
                count = gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET).count();
            }
            return count;
        }

        public void addItem(final AuthoritativeItem item) {
            if (JavaHandlerUtils.addSingleRelationship(item.asVertex(), it(),
                    Ontology.ITEM_IN_AUTHORITATIVE_SET)) {
                updateChildCountCache();
            }
        }
    }
    
}
