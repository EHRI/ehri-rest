package eu.ehri.project.models;

import com.tinkerpop.frames.*;

public interface Entity {

    @Property("name") public String getName();
    @Property("name") public void setName(String name);
    
    @Property("element_type") public String getType();

    @Property("identifier") public String getIdentifier();
    @Property("identifier") public String setIdentifier();
}



