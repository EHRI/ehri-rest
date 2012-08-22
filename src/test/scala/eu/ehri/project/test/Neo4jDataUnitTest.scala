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
      graph.getVertices.toList.length must_== TestData.nodes.length + 1 // including ROOT node
    }

    "contains a collection present in the test data" in new LoadedDB {
      val c = dataLoader.findTestElement("c3", classOf[DocumentaryUnit])
      c.getName mustEqual "Test Collection 3"
    }

    "contain some groups" in new LoadedDB {
      val groups = dataLoader.findTestElements(Group.isA, classOf[Group])
      groups mustNotEqual Nil
    }

    "contain some groups with users in" in new LoadedDB {
      val groups = dataLoader.findTestElements(Group.isA, classOf[Group])
      groups.head.getUsers.toList mustNotEqual Nil
    }
  }
  
  "Collections" should {
    "be held by a repository" in new LoadedDB {
      dataLoader.findTestElements(DocumentaryUnit.isA, classOf[DocumentaryUnit]).toList.filter { c =>
        c.getAgent == null
      } mustEqual Nil
    }
  }

  "The admin group" should {
    "contain some users" in new LoadedDB {
      val admin = dataLoader.findTestElement("adminGroup", classOf[Group])
      admin.getUsers.toList mustNotEqual Nil
    }
  }

  "The niod group" should {
    "have read-access permissions" in new LoadedDB {
      val accessor = dataLoader.findTestElement("niodGroup", classOf[Accessor])
      val cl = dataLoader.findTestElement("c1", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(cl, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual false
    }
  }
  
  "The c1 collection" should {
    "have read-write permissions for user 'Mike'" in new LoadedDB {
      val accessor = dataLoader.findTestElement("mike", classOf[Accessor])
      val cl = dataLoader.findTestElement("c1", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(cl, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }
  
  "The c3 collection" should {
    "have read-write permissions for user 'Tim'" in new LoadedDB {
      val accessor = dataLoader.findTestElement("tim", classOf[Accessor])
      val cl = dataLoader.findTestElement("c3", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(cl, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }
  
  "The c2 collection" should {
    "ensure that group perms override user perms" in new LoadedDB {
      val accessor = dataLoader.findTestElement("tim", classOf[Accessor])
      val cl = dataLoader.findTestElement("c2", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(cl, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }
  
  "The Mike user" should {
    "have an annotation" in new LoadedDB {
      val mike = dataLoader.findTestElement("mike", classOf[UserProfile])
      mike.getAnnotations.toList mustNotEqual Nil
    }
    
    "with the right body" in new LoadedDB {
      val mike = dataLoader.findTestElement("mike", classOf[UserProfile])
      mike.getAnnotations.head.getBody mustEqual "Hello Dolly!"
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
    val dataLoader = new DataLoader(graph)
    dataLoader.loadTestData
  }
}

