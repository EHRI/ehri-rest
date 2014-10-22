package eu.ehri.project.persistence;

import eu.ehri.project.models.base.Frame;

/**
 * Class representing a callback that is triggered when some
 * traversal condition is met.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface TraversalCallback {
    public void process(Frame vertexFrame, int depth, String relation, int relationIndex);
}
