package eu.ehri.project.test

import org.specs2.mutable._

import org.neo4j.test.TestGraphDatabaseFactory
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import eu.ehri.project.models._

import scala.collection.JavaConversions._


class GraphTest extends Specification {
  sequential
  "The Initial Database" should {
    "be empty (except for the ROOT node)" in new DB {
      graph.getVertices.toList.length mustEqual 1
    }
  }

  "The Loaded Database" should {
    "contains the right number of vertices" in new LoadedDB {
      graph.getVertices.toList.length must_== DataLoader.testNodes.length + 1 // including ROOT node
    }

    "contains a collection present in the test data" in new LoadedDB {
      val iter = graph.getVertices("identifier", "c3", classOf[Collection])
      iter.head.getName mustEqual "Test Collection 3"
    }

    "contain some groups" in new LoadedDB {
      val groups = graph.getVertices("isA", "group", classOf[Group])
      groups.head.getUsers.toList mustNotEqual Nil
    }
  }
  
  "Collections" should {
    "be held by a repository" in new LoadedDB {
      graph.getVertices("isA", "collection", classOf[Collection]).toList.filter { c =>
        c.getCHInstitution == null
      } mustEqual Nil
    }
  }

  "The admin group" should {
    "contain a user" in new LoadedDB {
      val groups = graph.getVertices("name", "admin", classOf[Group])
      groups.head.getUsers.toList mustNotEqual Nil
    }
  }

  "The niod group" should {
    "have read-access permissions" in new LoadedDB {
      val accessor = graph.getVertices("name", "niod", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c1", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual false
    }
  }
  
  "The c1 collection" should {
    "have read-write permissions for user 'Mike'" in new LoadedDB {
      val accessor = graph.getVertices("name", "Mike", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c1", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }
  
  "The c3 collection" should {
    "have read-write permissions for user 'Tim'" in new LoadedDB {
      val accessor = graph.getVertices("name", "Tim", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c3", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }
  
  "The c2 collection" should {
    "ensure that group perms override user perms" in new LoadedDB {
      val accessor = graph.getVertices("name", "Tim", classOf[Accessor]).head
      val c1 = graph.getVertices("identifier", "c2", classOf[Entity]).head
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }

  trait DB extends After {
    // Set up our database...
    val graph = new FramedGraph[Neo4jGraph](
      new Neo4jGraph(
        new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()))

    def after = Unit //graph.shutdown, not needed on impermanentDatabase
  }

  trait LoadedDB extends DB {
    new DataLoader(graph).loadTestData
  }
}

