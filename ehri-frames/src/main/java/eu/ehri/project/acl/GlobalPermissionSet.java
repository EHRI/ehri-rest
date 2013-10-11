package eu.ehri.project.acl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

/**
 * User: mike
 *
 * Convenience wrapper for the permission set data structure, which
 * looks like:
 *
 *  {
 *      contentType -> [perms...],
 *      ...
 *  }
 *
 *  NB: This is not currently an immutable data structure.
 */
public final class GlobalPermissionSet {

    final HashMultimap<ContentTypes,PermissionType> matrix;

    public GlobalPermissionSet(Multimap<ContentTypes,PermissionType> permissionMatrix) {
        matrix = HashMultimap.create(permissionMatrix);
    }

    public GlobalPermissionSet() {
        matrix = HashMultimap.create();
    }

    public static GlobalPermissionSet empty() {
        return new GlobalPermissionSet();
    }

    public void setContentType(ContentTypes contentType, Collection<PermissionType> permissionTypes) {
        matrix.putAll(contentType, permissionTypes);
    }

    public void setContentType(ContentTypes contentType, PermissionType ... permissionTypes) {
        matrix.putAll(contentType, Lists.newArrayList(permissionTypes));
    }

    public Collection<PermissionType> get(ContentTypes type) {
        return matrix.get(type);
    }

    public Map<ContentTypes,Collection<PermissionType>> asMap() {
        return matrix.asMap();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GlobalPermissionSet that = (GlobalPermissionSet) o;

        if (!matrix.equals(that.matrix)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public String toString() {
        return "<GlobalPermissions: " + matrix.toString() + ">";
    }
}
