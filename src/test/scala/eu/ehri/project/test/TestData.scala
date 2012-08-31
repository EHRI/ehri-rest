package eu.ehri.project.test

import eu.ehri.project.models._
import eu.ehri.project.models.base.AccessibleEntity
import eu.ehri.project.models.base.TemporalEntity
import eu.ehri.project.models.base.Annotator
import eu.ehri.project.models.base.Description
import eu.ehri.project.relationships._

object TestData {
  
  // Node data and associated descriptor
  case class NN(desc: String, data: Map[String,Any])
  
  val nodes = List(
    NN("c1",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c1",
        "name" -> "Test Collection 1")),
    NN("cd1",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c1",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 1")),
    NN("c2",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c2",
        "name" -> "Test Collection 2")),
    NN("cd2",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c2",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 2")),
    NN("c3",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c3",
        "name" -> "Test Collection 3")),
    NN("cd3",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c3",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 3")),
    NN("c4",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c4",
        "name" -> "Test Collection 4")),
    NN("cd4",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c4",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 4")),
    NN("dp1",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1940-01-01T00:00:01Z",
        "endDate" -> "1945-12-12T00:00:01Z")),
    NN("dp2",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1943-01-01T00:00:01Z",
        "endDate" -> "1945-12-12T00:00:01Z")),
    NN("r1",
      Map(
        "isA" -> EntityTypes.AGENT,
        "identifier" -> "r1",
        "name" -> "Repository 1")),
    NN("rd1",
      Map(
        "isA" -> EntityTypes.AGENT_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "A Repository called 1")),
    NN("a1",
      Map(
        "isA" -> EntityTypes.AUTHORITY,
        "identifier" -> "a1",
        "typeOfEntity" -> "person",
        "name" -> "Authority 1")),
    NN("ad1",
      Map(
        "isA" -> EntityTypes.AUTHORITY_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "An Authority called 1")),
    NN("a2",
      Map(
        "isA" -> EntityTypes.AUTHORITY,
        "typeOfEntity" -> "person",
        "identifier" -> "a2",
        "name" -> "Authority 2")),
    NN("ad2",
      Map(
        "isA" -> EntityTypes.AUTHORITY_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "An Authority called 2")),
    NN("adminGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> Group.ADMIN_GROUP_NAME)),
    NN("niodGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> "niod")),
    NN("kclGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> "kcl")),
    NN("mike",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 1,
        "name" -> "Mike")),
    NN("reto",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 2,
        "name" -> "Reto")),
    NN("tim",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 3,
        "name" -> "Tim")),
    NN("pd1",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1943",
        "endDate" -> "1943")),
    NN("ar1",
      Map(
        "isA" -> EntityTypes.ADDRESS,
        "streetAddress" -> "London")),
    NN("ann1",
      Map(
        "isA" -> EntityTypes.ANNOTATION,
        "body" -> "Hello Dolly!")),
    NN("ann2",
      Map(
        "isA" -> EntityTypes.ANNOTATION,
        "body" -> "Annotating my annotation!")))

  case class R(val src: String, val label: String, val dst: String, val data: Map[String,Any] = Map())
        
  val edges = List(
    // Collections owned by repository
    R("r1", Agent.HOLDS, "c1"),
    R("r1", Agent.HOLDS, "c2"),
    R("r1", Agent.HOLDS, "c3"),
    R("r1", Agent.HOLDS, "c4"),

    // C3 is a child of C2 and C2 of C1
    R("c2", DocumentaryUnit.CHILD_OF, "c1"),
    R("c3", DocumentaryUnit.CHILD_OF, "c2"),

    // Repository has an address
    R("r1", Agent.HAS_ADDRESS, "ar1"),

    // Descriptions describe entities
    R("cd1", Description.DESCRIBES, "c1"),
    R("cd2", Description.DESCRIBES, "c2"),
    R("cd3", Description.DESCRIBES, "c3"),
    R("cd4", Description.DESCRIBES, "c4"),
    R("rd1", Description.DESCRIBES, "r1"),
    R("ad1", Description.DESCRIBES, "a1"),
    R("ad2", Description.DESCRIBES, "a2"),

    // Collections have dates
    R("c1", TemporalEntity.HAS_DATE, "dp1"),
    R("c1", TemporalEntity.HAS_DATE, "dp2"),

    // Authorities create and are mentionedIn collections
    R("a1", Authority.CREATED, "c1"),
    R("a2", Authority.MENTIONED_IN, "c1"),

    // Users belong to groups
    R("mike", UserProfile.BELONGS_TO, "adminGroup"),
    R("tim", UserProfile.BELONGS_TO, "adminGroup"),
    R("mike", UserProfile.BELONGS_TO, "kclGroup"),
    R("reto", UserProfile.BELONGS_TO, "kclGroup"),
    R("tim", UserProfile.BELONGS_TO, "niodGroup"),

    // Access control
    R("c1", AccessibleEntity.ACCESS, "adminGroup", Map("read" -> true, "write" -> true)),
    R("c2", AccessibleEntity.ACCESS, "adminGroup", Map("read" -> true, "write" -> true)),
    R("c1", AccessibleEntity.ACCESS, "mike", Map("read" -> true, "write" -> false)),
    R("c2", AccessibleEntity.ACCESS, "tim", Map("read" -> true, "write" -> false)),
    R("c3", AccessibleEntity.ACCESS, "tim", Map("read" -> true, "write" -> true)),

    // Annotations
    R("mike", Annotator.HAS_ANNOTATION, "ann1"),
    R("ann1", Annotation.ANNOTATES, "c1", Map("timestamp" -> "2012-08-08T00:00:00Z", "field" -> "scopeAndContent")),
    R("tim", Annotator.HAS_ANNOTATION, "ann2"),
    R("ann2", Annotation.ANNOTATES, "ann1", Map("timestamp" -> "2012-08-08T00:00:00Z", "field" -> "scopeAndContent")))
}