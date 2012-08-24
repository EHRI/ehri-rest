package eu.ehri.project.acl

import eu.ehri.project.models._
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships._

import scala.collection.JavaConversions._

object Acl {

  /* 
   * Define a custom comparator for Access controls. Because this
   * is in implicit scope, it is used in the List[Access].max function.
   * FIXME: There's bound to be a better way to do this.
   */
  implicit object AccessOrdering extends Ordering[Access] {
    def compare(a: Access, b: Access): Int = {
      if (a.getWrite == b.getWrite && a.getRead == b.getRead) 0
      else if (a.getWrite && b.getWrite && a.getRead && !b.getRead) 1
      else if (a.getWrite && !b.getWrite && a.getRead && !b.getRead) 1
      else -1
    }
  }
  
  /*
   * Check if an accessor is admin or a member of Admin.
   */
  private def isAdmin(accessor: Accessor) = {
    (accessor :: accessor.getAllParents.toList).exists(_.getName == Group.ADMIN_GROUP_NAME)
  }

  /* We have to ascend the current accessors group
   * hierarchy looking for a groups that are contained
   * in the current entity's ACL list. Return the Access
   * relationship objects and see which one is most liberal.
   */
  private def searchPermissions(accessors: List[Accessor], ctrlGroups: List[(Access, Accessor)]): List[Access] = accessors match {
    // Termination condition on recursive function.
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

  /*
   * Find the Access permissions for a given entity and accessor. 
   */
  def getAccessControl(entity: AccessibleEntity, accessor: Accessor): Access = {
    // Admin can read/write everything
    if (isAdmin(accessor)) {
      new EntityAccessFactory().buildReadWrite(entity, accessor)
    } else {
      // Otherwise, check if there are specified permissions.
      // Build a Tuple of (AccessObject, UserOrGroup)
      // TODO: Zipping entity.getAccess and entity.getAccessors might
      // be more efficient here.
      val ctrlUserGroups = entity.getAccess.toList.map(entity => (entity, entity.getAccessor))
      // If there are no restrictions, default to read-only
      if (ctrlUserGroups.isEmpty) {
        new EntityAccessFactory().buildReadOnly(entity, accessor)
      } else {
        // If there are, search the Group hierarchy and find the most
        // powerful set of permissions contained therein...
        val ctrls = searchPermissions(List(accessor), ctrlUserGroups)
        if (ctrls.isEmpty)
          new EntityAccessFactory().buildNoAccess(entity, accessor)
        else
          ctrls.max
      }
    }
  }  
}

