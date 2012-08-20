package eu.ehri.project.models

import com.tinkerpop.frames._

trait Entity {
  @Property("name") def getName: String
  @Property("name") def setName(s: String)

}

