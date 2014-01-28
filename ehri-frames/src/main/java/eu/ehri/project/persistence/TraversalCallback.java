package eu.ehri.project.persistence;

import eu.ehri.project.models.base.Frame;

public interface TraversalCallback {
    public void process(Frame vertexFrame, int depth, String relation, int relationIndex);
}
