package eu.ehri.project.test

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.mapAsJavaMap
import org.neo4j.test.TestGraphDatabaseFactory
import org.specs2.mutable.After
import org.specs2.mutable.Specification
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.frames.FramedGraph
import eu.ehri.project.exceptions.ValidationError
import eu.ehri.project.models.base.AccessibleEntity
import eu.ehri.project.models.base.Accessor
import eu.ehri.project.models.base.AnnotatableEntity
import eu.ehri.project.models.base.Annotator
import eu.ehri.project.models.Agent
import eu.ehri.project.models.Annotation
import eu.ehri.project.models.Authority
import eu.ehri.project.models.Group
import eu.ehri.project.models.UserProfile
import eu.ehri.project.models.DocumentaryUnit
import eu.ehri.project.models.EntityTypes
import eu.ehri.project.persistance.BundleFactory
import eu.ehri.project.persistance.BundleDAO
import eu.ehri.project.persistance.Converter
import eu.ehri.project.models.DatePeriod
import eu.ehri.project.models.DocumentaryUnit

trait DB extends After {
  // Set up our database...
  val graph = new FramedGraph[Neo4jGraph](
    new Neo4jGraph(
      new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()))

  // Helper for converting between bundles and frames
  val converter = new Converter

  def after = Unit //graph.shutdown, not needed on impermanentDatabase
}

trait LoadedDB extends DB {
  val helper = new DataLoader(graph)
  helper.loadTestData
}

class GraphTest extends Specification {
  // FIXME: these tests need to run sequentially otherwise
  // bad things happen with the Neo4j test database.
  // It might be worth finding out why some time.
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
      val c = helper.findTestElement("c3", classOf[DocumentaryUnit])
      c.getName mustEqual "Test Collection 3"
    }

    "contain some groups" in new LoadedDB {
      val groups = helper.findTestElements(EntityTypes.GROUP, classOf[Group])
      groups mustNotEqual Nil
    }

    "contain some groups with users in" in new LoadedDB {
      val groups = helper.findTestElements(EntityTypes.GROUP, classOf[Group])
      groups.head.getUsers.toList mustNotEqual Nil
    }

  }

  "Collections" should {
    "be held by a repository" in new LoadedDB {
      helper.findTestElements(EntityTypes.DOCUMENTARY_UNIT, classOf[DocumentaryUnit]).toList.filter { c =>
        c.getAgent == null
      } mustEqual Nil
    }

    "and have a description" in new LoadedDB {
      helper.findTestElements(EntityTypes.DOCUMENTARY_UNIT, classOf[DocumentaryUnit]).toList.filter { c =>
        c.getDescriptions.isEmpty
      } mustEqual Nil
    }
  }

  "Collection c1" should {
    "have a creator a1" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val a1 = helper.findTestElement("a1", classOf[Authority])
      c1.getCreators.head mustEqual a1
    }

    "and a name access point a2" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val a2 = helper.findTestElement("a2", classOf[Authority])
      c1.getNameAccess.head mustEqual a2
    }
  }

  "Authority a1" should {
    "be the creator of collection c1" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val a1 = helper.findTestElement("a1", classOf[Authority])
      a1.getDocumentaryUnits.head mustEqual c1
    }
  }

  "Authority a2" should {
    "be a name access point for collection c1" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val a2 = helper.findTestElement("a2", classOf[Authority])
      a2.getMentionedIn.head mustEqual c1
    }
  }

  "The repository" should {
    "have an address" in new LoadedDB {
      val repo = helper.findTestElement("r1", classOf[Agent])
      repo.getAddresses.toList mustNotEqual Nil
    }

    "and have a description" in new LoadedDB {
      val repo = helper.findTestElement("r1", classOf[Agent])
      repo.getDescriptions.toList mustNotEqual Nil
    }
  }

  "The admin group" should {
    "contain some users" in new LoadedDB {
      val admin = helper.findTestElement("adminGroup", classOf[Group])
      admin.getUsers.toList mustNotEqual Nil
    }

    "be able to read/write everything" in new LoadedDB {
      val admin = helper.findTestElement("adminGroup", classOf[Accessor])
      val reto = helper.findTestElement("reto", classOf[Accessor])
      val c3 = helper.findTestElement("c3", classOf[AccessibleEntity])
      val retoAccess = eu.ehri.project.acl.Acl.getAccessControl(c3, reto)
      val adminAccess = eu.ehri.project.acl.Acl.getAccessControl(c3, admin)
      adminAccess.getRead mustEqual true
      adminAccess.getWrite mustEqual true
      retoAccess.getRead mustEqual false
      retoAccess.getWrite mustEqual false
    }
  }

  "The niod group" should {
    "have no access to items with admin-permissions" in new LoadedDB {
      val accessor = helper.findTestElement("niodGroup", classOf[Accessor])
      val c1 = helper.findTestElement("c1", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual false
      access.getWrite() mustEqual false
    }

    "but have read-only access to items with no specified permissions" in new LoadedDB {
      val accessor = helper.findTestElement("niodGroup", classOf[Accessor])
      val c4 = helper.findTestElement("c4", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(c4, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual false
    }
  }

  "The c1 collection" should {
    "have read-write permissions for user 'Mike'" in new LoadedDB {
      val accessor = helper.findTestElement("mike", classOf[Accessor])
      val c1 = helper.findTestElement("c1", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(c1, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }

    "have a child (c2)" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val c2 = helper.findTestElement("c2", classOf[DocumentaryUnit])
      c1.getChildren.head mustEqual c2
    }

    "and be the parent of c2" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val c2 = helper.findTestElement("c2", classOf[DocumentaryUnit])
      c2.getParent mustEqual c1
    }

    "and be the first ancestor of c2" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val c2 = helper.findTestElement("c2", classOf[DocumentaryUnit])
      c2.getAncestors.head mustEqual c1
    }

    "and be the an ancestor of c3" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val c2 = helper.findTestElement("c2", classOf[DocumentaryUnit])
      val c3 = helper.findTestElement("c3", classOf[DocumentaryUnit])
      c3.getAncestors.toList.length mustEqual 2
      c3.getAncestors.head mustEqual c2
      c3.getAncestors.last mustEqual c1
    }
  }

  "The c3 collection" should {
    "have read-write permissions for user 'Tim'" in new LoadedDB {
      val accessor = helper.findTestElement("tim", classOf[Accessor])
      val c3 = helper.findTestElement("c3", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(c3, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }

  "The c2 collection" should {
    "ensure that group perms override user perms" in new LoadedDB {
      val accessor = helper.findTestElement("tim", classOf[Accessor])
      val c2 = helper.findTestElement("c2", classOf[AccessibleEntity])
      val access = eu.ehri.project.acl.Acl.getAccessControl(c2, accessor)
      access.getRead() mustEqual true
      access.getWrite() mustEqual true
    }
  }

  "The Mike user" should {
    "have an annotation" in new LoadedDB {
      val mike = helper.findTestElement("mike", classOf[UserProfile])
      mike.getAnnotations.toList mustNotEqual Nil
    }

    "with the right body" in new LoadedDB {
      val mike = helper.findTestElement("mike", classOf[UserProfile])
      mike.getAnnotations.head.getBody mustEqual "Hello Dolly!"
    }

    "and the right target" in new LoadedDB {
      val mike = helper.findTestElement("mike", classOf[UserProfile])
      val c1 = helper.findTestElement("c1", classOf[AnnotatableEntity])
      mike.getAnnotations.head.getTarget mustEqual c1
    }

    "and the right context" in new LoadedDB {
      val mike = helper.findTestElement("mike", classOf[Annotator])
      val annotation = mike.getAnnotations.head
      annotation.getContext.head.getField mustEqual "scopeAndContent"
    }
  }

  "The first annotation" should {
    "be annotated by the second annotation" in new LoadedDB {
      val ann1 = helper.findTestElement("ann1", classOf[AnnotatableEntity])
      val ann2 = helper.findTestElement("ann2", classOf[Annotation])
      ann2.getTarget mustEqual ann1
    }

    "which has the right body" in new LoadedDB {
      val ann1 = helper.findTestElement("ann1", classOf[AnnotatableEntity])
      val ann2 = helper.findTestElement("ann2", classOf[Annotation])
      ann1.getAnnotations.head.getBody mustEqual ann2.getBody
    }
  }

  "Attempting to create a DocumentaryUnit bundle" should {
    "not crash and result in a document with the same name as the test data" in new DB {
      val m = Map("identifier" -> "c1", "name" -> "Collection 1");
      val bundle = new BundleFactory().buildBundle(m, classOf[DocumentaryUnit]);
      val persister = new BundleDAO[DocumentaryUnit](graph);
      val doc = persister.insert(bundle);
      doc.getName mustEqual m.getOrElse("name", "")
    }
  }

  "Attempting to update DocumentaryUnit" should {
    "change the node the right way" in new LoadedDB {
      val newName = "Another Name"
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val bundle = new BundleFactory[DocumentaryUnit]().fromFramedVertext(c1).setDataValue("name", newName);
      val persister = new BundleDAO[DocumentaryUnit](graph);
      val doc = persister.update(bundle);
      val c1again = helper.findTestElement("c1", classOf[DocumentaryUnit])
      c1again.getName mustEqual newName
    }

    "throw a validation error if we erase a needed value" in new LoadedDB {
      val newName = "Another Name"
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val oldName = c1.getName
      val bundle = new BundleFactory[DocumentaryUnit]().fromFramedVertext(c1).setDataValue("name", null);
      val persister = new BundleDAO[DocumentaryUnit](graph);
      persister.update(bundle) must throwA[ValidationError]
      val c1again = helper.findTestElement("c1", classOf[DocumentaryUnit])
      c1again.getName mustEqual oldName
    }
  }

  "Bundles" should {
    "be serialisable and deserializable" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val json = converter.vertexFrameToJson(c1);

      val bundle = converter.jsonToBundle(json)
      bundle.getId() mustEqual c1.asVertex().getId()
    }

    "allow resaving" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      c1.getDescriptions.toList.length mustEqual 1
      val bundle = converter.vertexFrameToBundle[DocumentaryUnit](c1)
      val persister = new BundleDAO[DocumentaryUnit](graph);
      val c1redux = persister.update(bundle)
      c1.getDescriptions.toList mustEqual c1redux.getDescriptions.toList
      // Check saving the bundle didn't add another item
      c1.getDescriptions.toList.length mustEqual 1
    }

    "allow resaving with subordinate changes" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      // Save an object as JSON
      val json = converter.vertexFrameToJson(c1)
      // Now change the object...
      val cd1 = c1.getDescriptions.toList.head
      c1.removeDescription(cd1)
      c1.getDescriptions.toList.length mustEqual 0
      // And restore it again from the bundle.
      val bundle = converter.jsonToBundle[DocumentaryUnit](json)
      val persister = new BundleDAO[DocumentaryUnit](graph);
      val c1redux = persister.update(bundle)
      // Tada: it's back again
      c1.getDescriptions.toList.length mustEqual 1
    }

    "delete removed subordinates" in new LoadedDB {
      val c1 = helper.findTestElement("c1", classOf[DocumentaryUnit])
      val bundle = converter.vertexFrameToBundle[DocumentaryUnit](c1)
      c1.getDatePeriods().toList.length mustEqual 2
      val c = bundle.getRelations.getCollection("hasDate")
      bundle.getRelations.remove("hasDate")
      bundle.getRelations.put("hasDate", c.toList.head)
      val persister = new BundleDAO[DocumentaryUnit](graph);
      val c1redux = persister.update(bundle)
      c1.getDatePeriods().toList.length mustEqual 1
    }
  }
}

