package eu.ehri.project.relationships;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.*;
import eu.ehri.project.models.*;

public interface Access {

    @Property("read") public Boolean getRead();
    @Property("read") public void setRead(Boolean canRead);
    @Property("write") public Boolean getWrite();
    @Property("write") public void setWrite(Boolean canWrite);

    @Range public Accessor getAccessor();
    @Domain public Entity getEntity();
}


