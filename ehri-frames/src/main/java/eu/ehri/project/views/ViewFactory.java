package eu.ehri.project.views;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.views.impl.CrudViews;
import eu.ehri.project.views.impl.LoggingCrudViews;


/**
 * Factory for creating crud, with or without logging.
 *
 * User: michaelb
 */
public class ViewFactory {

    public static <E extends AccessibleEntity> Crud<E> getCrudNoLogging(FramedGraph<?> graph,
            Class<E> cls, PermissionScope scope) {
        return new CrudViews<E>(graph, cls, scope);
    }

    public static <E extends AccessibleEntity> Crud<E> getCrudWithLogging(FramedGraph<?> graph,
            Class<E> cls, PermissionScope scope) {
        return new LoggingCrudViews<E>(graph, cls, scope);
    }

    public static <E extends AccessibleEntity> Crud<E> getCrudNoLogging(FramedGraph<?> graph,
            Class<E> cls) {
        return new CrudViews<E>(graph, cls);
    }

    public static <E extends AccessibleEntity> Crud<E> getCrudWithLogging(FramedGraph<?> graph,
            Class<E> cls) {
        return new LoggingCrudViews<E>(graph, cls);
    }
}