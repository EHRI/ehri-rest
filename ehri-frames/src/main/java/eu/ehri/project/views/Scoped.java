package eu.ehri.project.views;

import eu.ehri.project.models.base.PermissionScope;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Scoped<T> {
    public T withScope(PermissionScope scope);
}
