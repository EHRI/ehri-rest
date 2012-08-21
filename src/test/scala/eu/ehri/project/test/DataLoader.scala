package eu.ehri.project.test

import eu.ehri.project.core.Neo4jHelpers
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.blueprints.Vertex
import scala.collection.JavaConversions._

object DataLoader {
  val testNodes = List(
    ("c1",
      Map(
        "isA" -> "collection",
        "identifier" -> "c1",
        "name" -> "Test Collection 1")),
    ("c2",
      Map(
        "isA" -> "collection",
        "identifier" -> "c2",
        "name" -> "Test Collection 2")),
    ("c3",
      Map(
        "isA" -> "collection",
        "identifier" -> "c3",
        "name" -> "Test Collection 3")),
    ("r1",
      Map(
        "isA" -> "repository",
        "identifier" -> "r1",
        "name" -> "Repository 1")),
    ("a1",
      Map(
        "isA" -> "authority",
        "identifier" -> "a1",
        "name" -> "Authority 1")),
    ("a2",
      Map(
        "isA" -> "authority",
        "identifier" -> "a2",
        "name" -> "Authority 2")),
    ("g1",
      Map(
        "isA" -> "group",
        "name" -> "admin")),
    ("g2",
      Map(
        "isA" -> "group",
        "name" -> "niod")),
    ("g3",
      Map(
        "isA" -> "group",
        "name" -> "kcl")),
    ("u1",
      Map(
        "isA" -> "userprofile",
        "userId" -> 1,
        "name" -> "Mike")),
    ("u2",
      Map(
        "isA" -> "userprofile",
        "userId" -> 2,
        "name" -> "Reto")),
    ("u3",
      Map(
        "isA" -> "userprofile",
        "userId" -> 3,
        "name" -> "Tim")))
}

class DataLoader(val graph: FramedGraph[Neo4jGraph]) {
  val helper = new Neo4jHelpers(graph.getBaseGraph.getRawGraph)

  def createRelationship(src: Vertex, dst: Vertex, label: String, data: Map[String, Any] = Map()) = {
    helper.getOrCreateEdgeIndex(label)
    helper.createIndexedEdge(
      src.getId().asInstanceOf[java.lang.Long],
      dst.getId().asInstanceOf[java.lang.Long],
      label, data.asInstanceOf[Map[String, Object]])
  }

  // FIXME: This function is vulnerable to collisions!
  def addUserToGroup(userName: String, groupName: String) = {
    val g = graph.getBaseGraph.getVertices("name", groupName).toList.head
    val u = graph.getBaseGraph.getVertices("name", userName).toList.head
    createRelationship(u, g, "belongsTo")
  }

  def setSecurity(itemId: String, userOrGroupName: String, read: Boolean, write: Boolean) = {
    val g = graph.getBaseGraph.getVertices("name", userOrGroupName).toList.head
    val c = graph.getBaseGraph.getVertices("identifier", itemId).toList.head
    createRelationship(c, g, "access", Map("read" -> read, "write" -> write))
  }

  def loadTestData = {

    val tx = graph.getBaseGraph.getRawGraph.beginTx

    // Create all the nodes
    DataLoader.testNodes.foreach {
      case (desc, node) =>
        node.get("isA").map {
          case (index: String) =>
            helper.getOrCreateVertexIndex(index)
            helper.createIndexedVertex(node.asInstanceOf[Map[String, Object]], index)
        }
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
