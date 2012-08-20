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
    "contain the right number of vertices" in new CollectionDB {
      graph.getVertices.toList.length must_== testData.flatMap(_._2).toList.length + 3 // including ROOT node
    }

    "contain a collection present in the test data" in new CollectionDB {
      val iter = graph.getVertices("identifier", "c3", classOf[Collection])
      iter.head.getName mustEqual "Test Collection 3"
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

    val testData = Map(
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
      )
    )

    val ehri = new Neo4jHelpers(graph.getBaseGraph.getRawGraph)
    val tx = graph.getBaseGraph.getRawGraph.beginTx
    testData.map { case (index, list) =>
      ehri.createVertexIndex(index)

      list.map { entity =>
        ehri.createIndexedVertex(entity + ("isA" -> index), index)
      }
    }

    val upidx = "userprofile"
    val gpidx = "group"
    val user = Map("isA" -> upidx, "userId" -> "1", "name" -> "Mike")
    val group = Map("isA" -> gpidx, "name" -> "admin")


    // Add a user profile
    ehri.createVertexIndex(upidx)
    val userVertex = ehri.createIndexedVertex(user, upidx)
    ehri.createVertexIndex(gpidx)
    val groupVertex = ehri.createIndexedVertex(group, gpidx)

    ehri.createEdgeIndex("belongsTo")
    val access = ehri.createIndexedEdge(
        groupVertex.getId().asInstanceOf[java.lang.Long],
        userVertex.getId().asInstanceOf[java.lang.Long],
        "belongsTo", Map("read"->"true", "write"->"true"))


    tx.success()
  }
}

