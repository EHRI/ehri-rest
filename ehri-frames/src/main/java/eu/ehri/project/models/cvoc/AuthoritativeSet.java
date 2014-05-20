/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 *
 * @author linda
 */
@EntityType(EntityClass.AUTHORITATIVE_SET)
public interface AuthoritativeSet extends AccessibleEntity, IdentifiableEntity,
        PermissionScope, ItemHolder {

    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET, direction = Direction.IN)
    public Iterable<AuthoritativeItem> getAuthoritativeItems();

    @JavaHandler
    public void addItem(final AuthoritativeItem item);

    @JavaHandler
    public void updateChildCountCache();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, AuthoritativeSet {

        public void updateChildCountCache() {
            it().setProperty(CHILD_COUNT, gremlin().in(Ontology.ITEM_IN_AUTHORITATIVE_SET).count());
        }

        public Long getChildCount() {
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
