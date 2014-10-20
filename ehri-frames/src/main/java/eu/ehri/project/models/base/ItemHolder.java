package eu.ehri.project.models.base;

import com.tinkerpop.frames.modules.javahandler.JavaHandler;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface ItemHolder {
    public static final String CHILD_COUNT = "_childCount";

    public long getChildCount();
}
