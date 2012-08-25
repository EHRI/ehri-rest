package eu.ehri.project.acl;

import com.tinkerpop.blueprints.Edge;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships.Access;

public class EntityAccess implements Access {
    Boolean read;
    Boolean write;
    AccessibleEntity entity;
    Accessor accessor;

    protected EntityAccess(Boolean read, Boolean write,
            AccessibleEntity entity, Accessor accessor) {
        this.read = read;
        this.write = write;
        this.entity = entity;
        this.accessor = accessor;
    }

    public Boolean getRead() {
        return read;
    }

    public Boolean getWrite() {
        return write;
    }

    public void setRead(Boolean canRead) {
        this.read = canRead;
    }

    public void setWrite(Boolean canWrite) {
        this.write = canWrite;
    }

    public AccessibleEntity getEntity() {
        return entity;
    }

    public void setEntity(AccessibleEntity entity) {
        this.entity = entity;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    public Edge asEdge() {
        // TODO Auto-generated method stub
        return null;
    }
}
