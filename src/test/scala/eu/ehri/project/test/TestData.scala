package eu.ehri.project.test

import eu.ehri.project.models._
import eu.ehri.project.models.base.AccessibleEntity
import eu.ehri.project.models.base.TemporalEntity
import eu.ehri.project.models.base.Annotator
import eu.ehri.project.models.base.Description
import eu.ehri.project.relationships._

object TestData {
  val nodes = List(
    ("c1",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c1",
        "name" -> "Test Collection 1")),
    ("cd1",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c1",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 1")),
    ("c2",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c2",
        "name" -> "Test Collection 2")),
    ("cd2",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c2",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 2")),
    ("c3",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c3",
        "name" -> "Test Collection 3")),
    ("cd3",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c3",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 3")),
    ("c4",
      Map(
        "isA" -> EntityTypes.DOCUMENTARY_UNIT,
        "identifier" -> "c4",
        "name" -> "Test Collection 4")),
    ("cd4",
      Map(
        "isA" -> EntityTypes.DOCUMENT_DESCRIPTION,
        "identifier" -> "c4",
        "languageOfDescription" -> "en",
        "title" -> "A Collection called 4")),
    ("dp1",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1940-01-01T00:00:01Z",
        "endDate" -> "1945-12-12T00:00:01Z")),
    ("dp2",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1943-01-01T00:00:01Z",
        "endDate" -> "1945-12-12T00:00:01Z")),
    ("r1",
      Map(
        "isA" -> EntityTypes.AGENT,
        "identifier" -> "r1",
        "name" -> "Repository 1")),
    ("rd1",
      Map(
        "isA" -> EntityTypes.AGENT_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "A Repository called 1")),
    ("a1",
      Map(
        "isA" -> EntityTypes.AUTHORITY,
        "identifier" -> "a1",
        "typeOfEntity" -> "person",
        "name" -> "Authority 1")),
    ("ad1",
      Map(
        "isA" -> EntityTypes.AUTHORITY_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "An Authority called 1")),
    ("a2",
      Map(
        "isA" -> EntityTypes.AUTHORITY,
        "typeOfEntity" -> "person",
        "identifier" -> "a2",
        "name" -> "Authority 2")),
    ("ad2",
      Map(
        "isA" -> EntityTypes.AUTHORITY_DESCRIPTION,
        "languageOfDescription" -> "en",
        "title" -> "An Authority called 2")),
    ("adminGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> Group.ADMIN_GROUP_NAME)),
    ("niodGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> "niod")),
    ("kclGroup",
      Map(
        "isA" -> EntityTypes.GROUP,
        "name" -> "kcl")),
    ("mike",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 1,
        "name" -> "Mike")),
    ("reto",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 2,
        "name" -> "Reto")),
    ("tim",
      Map(
        "isA" -> EntityTypes.USER_PROFILE,
        "userId" -> 3,
        "name" -> "Tim")),
    ("pd1",
      Map(
        "isA" -> EntityTypes.DATE_PERIOD,
        "startDate" -> "1943",
        "endDate" -> "1943")),
    ("ar1",
      Map(
        "isA" -> EntityTypes.ADDRESS,
        "streetAddress" -> "London")),
    ("ann1",
      Map(
        "isA" -> EntityTypes.ANNOTATION,
        "body" -> "Hello Dolly!")),
    ("ann2",
      Map(
        "isA" -> EntityTypes.ANNOTATION,
        "body" -> "Annotating my annotation!")))

  val edges = List(
    // Collections owned by repository
    ("r1", Agent.HOLDS, "c1", Map()),
    ("r1", Agent.HOLDS, "c2", Map()),
    ("r1", Agent.HOLDS, "c3", Map()),
    ("r1", Agent.HOLDS, "c4", Map()),

    // C3 is a child of C2 and C2 of C1
    ("c2", DocumentaryUnit.CHILD_OF, "c1", Map()),
    ("c3", DocumentaryUnit.CHILD_OF, "c2", Map()),

    // Repository has an address
    ("r1", Agent.HAS_ADDRESS, "ar1", Map()),

    // Descriptions describe entities
    ("cd1", Description.DESCRIBES, "c1", Map()),
    ("cd2", Description.DESCRIBES, "c2", Map()),
    ("cd3", Description.DESCRIBES, "c3", Map()),
    ("cd4", Description.DESCRIBES, "c4", Map()),
    ("rd1", Description.DESCRIBES, "r1", Map()),
    ("ad1", Description.DESCRIBES, "a1", Map()),
    ("ad2", Description.DESCRIBES, "a2", Map()),

    // Collections have dates
    ("c1", TemporalEntity.HAS_DATE, "dp1", Map()),
    ("c1", TemporalEntity.HAS_DATE, "dp2", Map()),

    // Authorities create and are mentionedIn collections
    ("a1", Authority.CREATED, "c1", Map()),
    ("a2", Authority.MENTIONED_IN, "c1", Map()),

    // Users belong to groups
    ("mike", UserProfile.BELONGS_TO, "adminGroup", Map()),
    ("tim", UserProfile.BELONGS_TO, "adminGroup", Map()),
    ("mike", UserProfile.BELONGS_TO, "kclGroup", Map()),
    ("reto", UserProfile.BELONGS_TO, "kclGroup", Map()),
    ("tim", UserProfile.BELONGS_TO, "niodGroup", Map()),

    // Access control
    ("c1", AccessibleEntity.ACCESS, "adminGroup", Map("read" -> true, "write" -> true)),
    ("c2", AccessibleEntity.ACCESS, "adminGroup", Map("read" -> true, "write" -> true)),
    ("c1", AccessibleEntity.ACCESS, "mike", Map("read" -> true, "write" -> false)),
    ("c2", AccessibleEntity.ACCESS, "tim", Map("read" -> true, "write" -> false)),
    ("c3", AccessibleEntity.ACCESS, "tim", Map("read" -> true, "write" -> true)),

    // Annotations
    ("mike", Annotator.HAS_ANNOTATION, "ann1", Map()),
    ("ann1", Annotation.ANNOTATES, "c1", Map("timestamp" -> "2012-08-08T00:00:00Z", "field" -> "scopeAndContent")),
    ("tim", Annotator.HAS_ANNOTATION, "ann2", Map()),
    ("ann2", Annotation.ANNOTATES, "ann1", Map("timestamp" -> "2012-08-08T00:00:00Z", "field" -> "scopeAndContent")))
}