package eu.ehri.project.models;

import com.tinkerpop.frames.*;


interface Authority extends Entity {
  @Property("type_of_entity") public String getTypeOfEntity();
}

