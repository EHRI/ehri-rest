package eu.ehri.project.api;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.api.impl.ApiImpl;

public class ApiFactory {

    public static Api noLogging(FramedGraph<?> graph, Accessor accessor) {
        return new ApiImpl(graph, accessor, SystemScope.getInstance(), false);
    }

    public static Api withLogging(FramedGraph<?> graph, Accessor accessor) {
        return new ApiImpl(graph, accessor, SystemScope.getInstance(), true);
    }
}
