package eu.ehri.project.acl;

import eu.ehri.project.models.Accessor;
import eu.ehri.project.models.AccessibleEntity;
import eu.ehri.project.relationships.Access;

public class EntityAccessFactory {
    public EntityAccessFactory() {
    }

    public Access buildReadWrite(AccessibleEntity entity, Accessor accessor) {
        return new EntityAccess(true, true, entity, accessor);
    }

    public Access buildReadOnly(AccessibleEntity entity, Accessor accessor) {
        return new EntityAccess(true, false, entity, accessor);
    }

    public Access buildNoAccess(AccessibleEntity entity, Accessor accessor) {
        return new EntityAccess(false, false, entity, accessor);
    }
}
