package eu.ehri.project.test

import org.specs2.mutable._

import org.neo4j.test.TestGraphDatabaseFactory
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import eu.ehri.project.models._
import eu.ehri.project.core.Neo4jHelpers

import scala.collection.JavaConversions._

import com.codahale.jerkson.Json

class GraphTest extends Specification {
  sequential
  "The Initial Database" should {
    "be empty" in new DB {
      !graph.getVertices.iterator.hasNext
    }
  }

  "The Loaded Database" should {
    "contains the right number of vertices" in new CollectionDB {
      graph.getVertices.toList.length must_== DataLoader.testNodes.length + 1 // including ROOT node
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

    DataLoader.loadTestData(graph)
  }
}

