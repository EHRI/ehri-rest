package eu.ehri.project.test

import org.specs2.mutable._

import org.neo4j.test.TestGraphDatabaseFactory
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import eu.ehri.project.models._
import eu.ehri.project.core.Neo4jHelpers

import scala.collection.JavaConversions._


class GraphTest extends Specification {
  sequential
  "The Initial Database" should {
    "be empty" in new DB {
      ! graph.getVertices.iterator.hasNext
    }
  }

  "The Loaded Database" should {
    "contains the right number of vertices" in new CollectionDB {
      graph.getVertices.toList.length must_== testNodes.flatMap(_._2).toList.length + 1 // including ROOT node
    }

    "contains a collection present in the test data" in new CollectionDB {
      val iter = graph.getVertices("identifier", "c3", classOf[Collection])
      iter.head.getName mustEqual "Test Collection 3"
    }
  }

  "The Group" should {
    "contain some users" in new CollectionDB {
      val groups = graph.getVertices("isA", "group", classOf[Group])
      groups.head.getUsers.iterator.hasNext
    }
    "with the first being the admin group" in new CollectionDB {
      val groups = graph.getVertices("isA", "group", classOf[Group])
      "admin" mustEqual groups.head.getName
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

    val testNodes = Map(
      "collection" -> List(
        Map(
          "identifier" -> "c1",
          "name" -> "Test Collection 1"
        ),
        Map(
          "identifier" -> "c2",
          "name" -> "Test Collection 2"
        ),
        Map(
          "identifier" -> "c3",
          "name" -> "Test Collection 3"
        )
      ),
      "repository" -> List(
        Map(
          "identifier" -> "r1",
          "name" -> "Repository 1"
        )
      ),
      "authority" -> List(
        Map(
          "identifier" -> "a1",
          "name" -> "Authority 1"
        ),
        Map(
          "identifier" -> "a2",
          "name" -> "Authority 2"
        )
      ),
      "group" -> List(
        Map(
          "name" -> "admin"
        ),
        Map(
          "name" -> "niod"
        )
      ),
      "userprofile" -> List(
        Map(
          "userId" -> "1",
          "name" -> "Mike"
        ),
        Map(
          "userId" -> "2",
          "name" -> "Reto"
        )
      )
    )

    val testEdges = Map(
      
    )

    val ehri = new Neo4jHelpers(graph.getBaseGraph.getRawGraph)
    val tx = graph.getBaseGraph.getRawGraph.beginTx
    testNodes.map { case (index, list) =>
      ehri.createVertexIndex(index)

      list.map { entity =>
        ehri.createIndexedVertex(entity + ("isA" -> index), index)
      }
    }

    List("belongsTo").map { label =>
      ehri.createEdgeIndex(label)
      graph.getBaseGraph.getVertices("isA", "group").map { group =>
        graph.getBaseGraph.getVertices("isA", "userprofile").map { user =>
          ehri.createIndexedEdge(
            group.getId().asInstanceOf[java.lang.Long],
            user.getId().asInstanceOf[java.lang.Long],
            label, Map("read" -> "true", "write" -> "true")
          )
        }
      }
    }

    tx.success()
  }
}

