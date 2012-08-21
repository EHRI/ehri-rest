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
  private def ascendGroupHierarchy(accessors: List[Accessor], ctrlGroups: List[(Access, Accessor)]
    ): List[Access] = accessors match {
    // accessor has no parents, so it's not allowed...
    case Nil => Nil
    case accessors => {
      // get the list of Access objects at this level
      val intersection: List[Access] = ctrlGroups.filter(t => accessors.contains(t._2)).map(t => t._1)
      // add it to the list of Access objects in parent groups
      intersection ++ accessors.flatMap(a => ascendGroupHierarchy(a.getParents.toList, ctrlGroups))
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
      val ctrls = ascendGroupHierarchy(List(accessor), ctrlUserGroups)
      if (ctrls.isEmpty)
        // TODO: How do we lock down an object completely???
        defaultAccess
      else
        ctrls.max // return the most elevated access control
    }
  }
}

