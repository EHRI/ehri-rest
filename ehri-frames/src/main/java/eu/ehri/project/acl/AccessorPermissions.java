package eu.ehri.project.acl;

import com.google.common.collect.ImmutableMap;
import eu.ehri.project.models.base.Accessor;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.Map;

/**
 * A pairing of an accessor and their non-inherited permissions.
 */
class AccessorPermissions<T> {
    final Accessor accessor;
    final T permissionSet;

    public AccessorPermissions(Accessor accessor, T permissionSet) {
        this.accessor = accessor;
        this.permissionSet = permissionSet;
    }

    @JsonValue
    public Map<String, T> asMap() {
        return ImmutableMap.of(accessor.getId(), permissionSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessorPermissions that = (AccessorPermissions) o;

        return accessor.equals(that.accessor)
                && permissionSet.equals(that.permissionSet);

    }

    @Override
    public int hashCode() {
        int result = accessor.hashCode();
        result = 31 * result + permissionSet.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "<" + accessor.getId() + " " + permissionSet + ">";
    }
}
