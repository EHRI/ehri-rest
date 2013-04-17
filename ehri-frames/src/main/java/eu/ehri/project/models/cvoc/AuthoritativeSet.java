/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 *
 * @author linda
 */
@EntityType(EntityClass.AUTHORITATIVE_SET)
public interface AuthoritativeSet extends AccessibleEntity, IdentifiableEntity, PermissionScope {
    public static final String IN_SET = "inAuthoritativeList";

    @Adjacency(label = IN_SET, direction = Direction.IN)
    public Iterable<AuthoritativeItem> getAuthoritativeItems();

    @Adjacency(label = IN_SET, direction = Direction.IN)
    public void addAuthoritativeItem(final AuthoritativeItem authoritativeItem);
    
}
