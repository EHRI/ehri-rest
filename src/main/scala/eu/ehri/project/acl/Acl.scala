package eu.ehri.project.acl

import eu.ehri.project.models._
import eu.ehri.project.relationships._

import scala.collection.JavaConversions._

object Acl {

  // define a custom comparator for Access controls
  implicit object AccessOrdering extends Ordering[Access] {
    def compare(a: Access, b: Access): Int = {
      if (a.getWrite == b.getWrite && a.getRead == b.getRead) 0
      else if (a.getWrite && b.getWrite && a.getRead && !b.getRead) 1
      else if (a.getWrite && !b.getWrite && a.getRead && !b.getRead) 1
      else -1
    }
  }

  // We have to ascend the current accessors group
  // hierarchy looking for a groups that are contained
  // in the current entity's ACL list. Return the Access
  // relationship objects and see which one is most liberal.
  private def searchPermissions(accessors: List[Accessor], ctrlGroups: List[(Access, Accessor)]): List[Access] = accessors match {
    // Termination condition on recursive function
    case Nil => Nil
    case _ => {
      // Get the list of Access objects *at this level*.
      // This is the intersection of the current accessors,
      // and the accessors specified on the entity's security.
      val intersection: List[Access] = ctrlGroups.filter(t => accessors.contains(t._2)).map(t => t._1)
      // Recurse through the current accessor's parents and
      // add any parent users/groups that are given access.
      intersection ++ accessors.flatMap(a => searchPermissions(a.getParents.toList, ctrlGroups))
    }
  }

  def getAccessControl(entity: Entity, accessor: Accessor): Access = {
    var defaultAccess = new EntityAccessFactory().buildReadOnly(entity, accessor)
    // Build a tuple of the entities access relationships, and the
    // respective groups they point to...
    val ctrlUserGroups = entity.getAccess.iterator.toList.map(entity => (entity, entity.getAccessor))
    if (ctrlUserGroups.isEmpty) {
      defaultAccess // default read-only access
    } else {
      val ctrls = searchPermissions(List(accessor), ctrlUserGroups)
      if (ctrls.isEmpty)
        // TODO: How do we lock down an object completely???
        defaultAccess
      else
        ctrls.max // return the most elevated access control
    }
  }
}

