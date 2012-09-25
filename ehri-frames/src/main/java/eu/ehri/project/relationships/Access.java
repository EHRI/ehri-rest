package eu.ehri.project.relationships;

import com.tinkerpop.frames.Domain;
import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.Range;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

public interface Access extends EdgeFrame {

    @Property("read")
    public Boolean getRead();

    @Property("read")
    public void setRead(Boolean canRead);

    @Property("write")
    public Boolean getWrite();

    @Property("write")
    public void setWrite(Boolean canWrite);

    @Range
    public Accessor getAccessor();

    @Domain
    public AccessibleEntity getEntity();
}
