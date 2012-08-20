package eu.ehri.project.relationships;

import com.tinkerpop.frames.*;

public interface Access {

    @Property("read") public Boolean getRead();
    @Property("read") public void setRead(Boolean canRead);
    @Property("write") public Boolean getWrite();
    @Property("write") public Boolean setWrite(Boolean canWrite);
}


