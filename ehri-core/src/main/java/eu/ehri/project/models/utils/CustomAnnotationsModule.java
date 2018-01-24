package eu.ehri.project.models.utils;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.frames.FramedGraphConfiguration;
import com.tinkerpop.frames.modules.AbstractModule;
import com.tinkerpop.frames.modules.Module;

public class CustomAnnotationsModule extends AbstractModule implements Module {
    @Override
    public void doConfigure(FramedGraphConfiguration config) {
        config.addMethodHandler(new UniqueAdjacencyAnnotationHandler());
    }
}
