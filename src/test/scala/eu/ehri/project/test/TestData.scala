package eu.ehri.project.test

import eu.ehri.project.models._
import eu.ehri.project.relationships._

object TestData {
  val nodes = List(
    ("c1",
      Map(
        "isA" -> DocumentaryUnit.isA,
        "identifier" -> "c1",
        "name" -> "Test Collection 1")),
    ("cd1",
      Map(
        "isA" -> DocumentDescription.isA,
        "title" -> "A Collection called 1")),
    ("c2",
      Map(
        "isA" -> DocumentaryUnit.isA,
        "identifier" -> "c2",
        "name" -> "Test Collection 2")),
    ("cd2",
      Map(
        "isA" -> DocumentDescription.isA,
        "title" -> "A Collection called 2")),
    ("c3",
      Map(
        "isA" -> DocumentaryUnit.isA,
        "identifier" -> "c3",
        "name" -> "Test Collection 3")),
    ("cd3",
      Map(
        "isA" -> DocumentDescription.isA,
        "title" -> "A Collection called 3")),
    ("r1",
      Map(
        "isA" -> Agent.isA,
        "identifier" -> "r1",
        "name" -> "Repository 1")),
    ("rd1",
      Map(
        "isA" -> AgentDescription.isA,
        "title" -> "A Repository called 1")),
    ("a1",
      Map(
        "isA" -> Authority.isA,
        "identifier" -> "a1",
        "name" -> "Authority 1")),
    ("ad1",
      Map(
        "isA" -> AuthorityDescription.isA,
        "title" -> "An Authority called 1")),
    ("a2",
      Map(
        "isA" -> Authority.isA,
        "identifier" -> "a2",
        "name" -> "Authority 2")),
    ("ad2",
      Map(
        "isA" -> AuthorityDescription.isA,
        "title" -> "An Authority called 2")),
    ("adminGroup",
      Map(
        "isA" -> Group.isA,
        "name" -> "admin")),
    ("niodGroup",
      Map(
        "isA" -> Group.isA,
        "name" -> "niod")),
    ("kclGroup",
      Map(
        "isA" -> Group.isA,
        "name" -> "kcl")),
    ("mike",
      Map(
        "isA" -> UserProfile.isA,
        "userId" -> 1,
        "name" -> "Mike")),
    ("reto",
      Map(
        "isA" -> UserProfile.isA,
        "userId" -> 2,
        "name" -> "Reto")),
    ("tim",
      Map(
        "isA" -> UserProfile.isA,
        "userId" -> 3,
        "name" -> "Tim")),
    ("pd1",
      Map(
        "isA" -> DatePeriod.isA,
        "startDate" -> "1943",
        "endDate" -> "1943")),
    ("ar1",
      Map(
        "isA" -> Address.isA,
        "streetAddress" -> "London")),
    ("ann1",
      Map(
        "isA" -> Annotation.isA,
        "body" -> "Hello Dolly!")))
        
        
  val edges = List(
    // Collections owned by repository
    ("r1", Agent.HOLDS, "c1", Map()),
    ("r1", Agent.HOLDS, "c2", Map()),
    ("r1", Agent.HOLDS, "c3", Map()),
    
    // Descriptions describe entities
    ("cd1", Description.DESCRIBES, "c1", Map()),
    ("cd2", Description.DESCRIBES, "c2", Map()),
    ("cd3", Description.DESCRIBES, "c3", Map()),
    ("rd1", Description.DESCRIBES, "r1", Map()),
    ("ad1", Description.DESCRIBES, "a1", Map()),
    ("ad2", Description.DESCRIBES, "a2", Map()),
    
    // Users belong to groups
    ("mike", UserProfile.BELONGS_TO, "adminGroup", Map()),
    ("reto", UserProfile.BELONGS_TO, "adminGroup", Map()),
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
    ("mike", Annotator.HAS_ANNOTATION, "ann1", Map("timestamp" -> "2012-08-08T00:00:00Z", "field" -> "scopeAndContent"))
    
  )
}