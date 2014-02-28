package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;

import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclIndexIterable<T extends Element> implements Iterable<Index<T>> {
    protected Iterable<Index<T>> iterable;
    private final AclGraph graph;

    public AclIndexIterable(final Iterable<Index<T>> iterable, final AclGraph graph) {
        this.iterable = iterable;
        this.graph = graph;
    }

    public Iterator<Index<T>> iterator() {
        return new Iterator<Index<T>>() {
            private final Iterator<Index<T>> itty = iterable.iterator();

            public void remove() {
                this.itty.remove();
            }

            public boolean hasNext() {
                return this.itty.hasNext();
            }

            public Index<T> next() {
                return new AclIndex<T>(this.itty.next(), graph);
            }
        };
    }
}
