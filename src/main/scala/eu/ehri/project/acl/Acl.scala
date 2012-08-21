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

  def getAccessControl(entity: Entity, accessor: Accessor): Access = {
    var access = new EntityAccessFactory().buildReadOnly(entity, accessor)
    // Build a tuple of the entities access relationships, and the
    // respective groups they point to...
    val ctrlUserGroups = entity.getAccess.iterator.toList.map(entity => (entity, entity.getAccessor))
    // we have to ascend the current accessors group
    // hierarchy looking for a group that might be contained
    // in the current entity's ACL list.
    def ascendGroupHierarchy(parents: List[Accessor]): List[Access] = parents match {
      // accessor has no parents, so it's not allowed...
      case Nil => Nil
      case parents => {
        val intersection = ctrlUserGroups.flatMap { case (ctrl, userOrGroup) =>
          if (parents.contains(userOrGroup)) List(ctrl)
          else Nil
        }
        intersection ++ parents.flatMap(p => ascendGroupHierarchy(p.getParents.toList))
      }
    }
    val ctrls = ascendGroupHierarchy(List(accessor))
    if (ctrls.isEmpty) access
    else ctrls.max
  }
}

