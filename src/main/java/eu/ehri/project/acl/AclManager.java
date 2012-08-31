package eu.ehri.project.acl;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.relationships.Access;

public class AclManager {

    /*
     * Check if an accessor is admin or a member of Admin.
     */
    public static Boolean isAdmin(Accessor accessor) {
        if (accessor.getName().equals(Group.ADMIN_GROUP_NAME))
            return true;
        for (Accessor acc : accessor.getAllParents()) {
            if (acc.getName().equals(Group.ADMIN_GROUP_NAME))
                return true;
        }
        return false;
    }
    
    /* We have to ascend the current accessors group
     * hierarchy looking for a groups that are contained
     * in the current entity's ACL list. Return the Access
     * relationship objects and see which one is most liberal.
     */
    public static List<Access> searchPermissions(List<Accessor> accessing, List<Access> allowedCtrls, List<Accessor> allowedAccessors) {
        assert allowedCtrls.size() == allowedAccessors.size();
        if (accessing.size() == 0) {
            return new ArrayList<Access>();
        } else {
            List<Access> intersection = new ArrayList<Access>();
            for (int i = 0; i < allowedCtrls.size(); i++) {
                if (accessing.contains(allowedAccessors.get(i))) {
                    intersection.add(allowedCtrls.get(i));
                }
            }
            
            List<Access> parentPerms = new ArrayList<Access>();
            parentPerms.addAll(intersection);
            for (Accessor acc : accessing) {
                List<Accessor> parents = new ArrayList<Accessor>();
                for (Accessor parent : acc.getAllParents())
                    parents.add(parent);
                parentPerms.addAll(searchPermissions(parents, allowedCtrls, allowedAccessors));
            }
            
            return parentPerms;
        }
    }
    
    /**
     * Find the access permissions for a given accessor and entity. 
     * @param accessor
     * @param entity
     * @return 
     * @return
     */
    public static Access getAccessControl(Accessor accessor, AccessibleEntity entity) {
        // Admin can read/write everything
        if (isAdmin(accessor))
            return new EntityAccessFactory().buildReadWrite(entity, accessor);
        
       // Otherwise, check if there are specified permissions.
       List<Access> accessCtrls = new ArrayList<Access>();
       List<Accessor> accessors = new ArrayList<Accessor>();
       for (Access access : entity.getAccess()) {
           accessCtrls.add(access);
           accessors.add(access.getAccessor());
       }
           
       if (accessCtrls.size() == 0) {
           return new EntityAccessFactory().buildReadOnly(entity, accessor);
       } else {
           // If there are, search the Group hierarchy and find the most
           // powerful set of permissions contained therein...
           List<Accessor> initial = new ArrayList<Accessor>();
           initial.add(accessor);
           List<Access> ctrls = searchPermissions(initial, accessCtrls, accessors);
           if (ctrls.size() == 0) {
               return new EntityAccessFactory().buildNoAccess(entity, accessor);
           } else {
               return Collections.max(ctrls, new Comparator<Access>() {
                   // FIXME: Find a better, more elegant way of doing this comparison
                   // between two access objects. We can't just implement a custom equals
                   // method because we might sometimes be comparing an Edge interface, and
                   // other times a synthetic factory-made object. Suggestions on a postcard.
                   public int compare(Access a, Access b) {
                       if (a.getWrite() == b.getWrite() && a.getRead() == b.getRead())
                           return 0;
                       else if (a.getWrite() && b.getWrite() && a.getRead() && !b.getRead())
                           return 1;
                       else if (a.getWrite() && !b.getWrite() && a.getRead() && !b.getRead())
                           return 1;
                       else return -1;             
                   }
               });
           }
       }           
    }
}
