package eu.ehri.project.test

import org.specs2.mutable._

import org.neo4j.test.TestGraphDatabaseFactory
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import eu.ehri.project.models._
import eu.ehri.project.core.Neo4jHelpers

import scala.collection.JavaConversions._
import scalaj.collection.Imports._

import org.neo4j.geoff
import com.codahale.jerkson.Json


class GraphTest extends Specification {
  sequential
  "The Initial Database" should {
    "be empty" in new DB {
      ! graph.getVertices.iterator.hasNext
    }
  }

  "The Loaded Database" should {
    "contains the right number of vertices" in new CollectionDB {
      graph.getVertices.toList.length must_== testNodes.length + 1 // including ROOT node
    }

    "contains a collection present in the test data" in new CollectionDB {
      val iter = graph.getVertices("identifier", "c3", classOf[Collection])
      iter.head.getName mustEqual "Test Collection 3"
    }

    "contain some groups" in new CollectionDB {
      val groups = graph.getVertices("isA", "group", classOf[Group])
      groups.head.getUsers.iterator.hasNext
    }
  }

  "The first Group" should {
    "contain a user" in new CollectionDB {
      val groups = graph.getVertices("isA", "group", classOf[Group])
      groups.head.getUsers.iterator.hasNext
    }
    
    "have read-access permissions" in new CollectionDB {
      val accessor = graph.getVertices("name", "niod", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c1", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      println(access.getRead, access.getWrite)
      access.getRead() mustEqual true
      access.getWrite() mustEqual false
    }

    "have read-write permissions for a user" in new CollectionDB {
      val accessor = graph.getVertices("name", "Mike", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c1", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      println(access.getRead, access.getWrite)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }

    "have read-write permissions for a user in that group" in new CollectionDB {
      val accessor = graph.getVertices("name", "Tim", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c3", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      println(access.getRead, access.getWrite)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }

    "ensure that group perms override user perms" in new CollectionDB {
      val accessor = graph.getVertices("name", "Tim", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c2", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      println(access.getRead, access.getWrite)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }

  trait DB extends After {
    // Set up our database...
    val graph = new FramedGraph[Neo4jGraph](
      new Neo4jGraph(
        new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()))

    def after = Unit //graph.shutdown
  }

  trait CollectionDB extends DB {

    val testNodes = List(
      ("c1", 
      Map(
        "isA" -> "collection",
        "identifier" -> "c1",
        "name" -> "Test Collection 1"
      )),
      ("c2",
      Map(
        "isA" -> "collection",
        "identifier" -> "c2",
        "name" -> "Test Collection 2"
      )),
      ("c3",
      Map(
        "isA" -> "collection",
        "identifier" -> "c3",
        "name" -> "Test Collection 3"
      )),
      ("r1",
      Map(
        "isA" -> "repository",
        "identifier" -> "r1",
        "name" -> "Repository 1"
      )),
      ("a1",
      Map(
        "isA" -> "authority",
        "identifier" -> "a1",
        "name" -> "Authority 1"
      )),
      ("a2",
      Map(
        "isA" -> "authority",
        "identifier" -> "a2",
        "name" -> "Authority 2"
      )),
      ("g1",
      Map(
        "isA" -> "group",
        "name" -> "admin"
      )),
      ("g2",
      Map(
        "isA" -> "group",
        "name" -> "niod"
      )),
      ("g3",
      Map(
        "isA" -> "group",
        "name" -> "kcl"
      )),
      ("u1",
      Map(
        "isA" -> "userprofile",
        "userId" -> 1,
        "name" -> "Mike"
      )),
      ("u2",
      Map(
        "isA" -> "userprofile",
        "userId" -> 2,
        "name" -> "Reto"
      )),
      ("u3",
      Map(
        "isA" -> "userprofile",
        "userId" -> 3,
        "name" -> "Tim"
      ))
    )

    // Test edges should specify a key/value for uniquely
    // identifying both source and target node, the relationship
    // label, and any accompanying data.
    val testEdges = List(
      ("u1", "belongsTo", "g1"),
      ("u2", "belongsTo", "g1"),
      ("r1", "holds", "c1"),
      ("r1", "holds", "c2"),
      ("r1", "holds", "c3"),
      ("a1", "created", "c1"),
      ("a2", "mentionedIn", "c2")
    )

    val ehri = new Neo4jHelpers(graph.getBaseGraph.getRawGraph)
    val tx = graph.getBaseGraph.getRawGraph.beginTx
    testNodes.foreach { case(desc, node) =>
      node.get("isA").map { case(index: String) =>
        ehri.getOrCreateVertexIndex(index)
        ehri.createIndexedVertex(node.asInstanceOf[Map[String,Object]], index)
      }
    }

    // FIXME: This function is vulnerable to collisions!
    def addUserToGroup(userName: String, groupName: String) = {
      val label = "belongsTo"
      ehri.getOrCreateEdgeIndex(label)

      // add Mike to admin group
      val g = graph.getBaseGraph.getVertices("name", groupName).toList.head
      val u = graph.getBaseGraph.getVertices("name", userName).toList.head
      ehri.createIndexedEdge(
        u.getId().asInstanceOf[java.lang.Long],
        g.getId().asInstanceOf[java.lang.Long],
        label, Map[String,Object]())
    }

    def setSecurity(itemId: String, userOrGroupName: String, read: Boolean, write: Boolean) = {
      val label = "access"
      ehri.getOrCreateEdgeIndex(label)

      val g = graph.getBaseGraph.getVertices("name", userOrGroupName).toList.head
      val c = graph.getBaseGraph.getVertices("identifier", itemId).toList.head
      ehri.createIndexedEdge(
        c.getId().asInstanceOf[java.lang.Long],
        g.getId().asInstanceOf[java.lang.Long],
        label, Map("read" -> read, "write" -> write).asInstanceOf[Map[String,Object]]
      )
    }
    
    addUserToGroup("Mike", "admin")
    addUserToGroup("Mike", "kcl")
    addUserToGroup("Tim", "niod")
    addUserToGroup("Tim", "admin")
    addUserToGroup("Reto", "kcl")

    setSecurity("c1", "Mike", true, true)
    setSecurity("c2", "kcl", true, false)
    setSecurity("c2", "admin", true, true)
    setSecurity("c2", "Tim", false, false) // Tim belongs to admin, so this should be overridden
    setSecurity("c3", "niod", true, true)

    tx.success()
  }
}

