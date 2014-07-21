package eu.ehri.project.acl.wrapper;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.StringFactory;


/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AclIndex<T extends Element> implements Index<T> {

    private final AclGraph<?> aclGraph;
    private final Index<T> baseIndex;

    protected AclIndex(Index<T> baseIndex, AclGraph<?> aclGraph) {
        this.baseIndex = baseIndex;
        this.aclGraph = aclGraph;
    }

    @Override
    public String getIndexName() {
        return baseIndex.getIndexName();
    }

    @Override
    public Class<T> getIndexClass() {
        return baseIndex.getIndexClass();
    }

    @Override
    public void put(String s, Object o, T element) {
        Element ele = (element instanceof AclElement)
                ? ((AclElement) element).baseElement : element;
        baseIndex.put(s, o, (T)ele);
    }

    @Override
    public CloseableIterable<T> get(final String key, final Object value) {
        if (Vertex.class.isAssignableFrom(this.getIndexClass())) {
            return (CloseableIterable<T>) new AclVertexIterable((Iterable<Vertex>) baseIndex.get(key, value), aclGraph);
        } else {
            return (CloseableIterable<T>) new AclEdgeIterable((Iterable<Edge>) baseIndex.get(key, value), aclGraph);
        }
    }

    @Override
    public CloseableIterable<T> query(final String key, final Object query) {
        if (Vertex.class.isAssignableFrom(this.getIndexClass())) {
            return (CloseableIterable<T>) new AclVertexIterable((Iterable<Vertex>) baseIndex.query(key, query), aclGraph);
        } else {
            return (CloseableIterable<T>) new AclEdgeIterable((Iterable<Edge>) baseIndex.query(key, query), aclGraph);
        }
    }


    public String toString() {
        return StringFactory.indexString(this);
    }

    @Override
    public long count(String s, Object o) {
        return Iterables.size(get(s, o));
    }

    @Override
    public void remove(String s, Object o, Element element) {
        Element ele = (element instanceof AclElement)
                ? ((AclElement) element).baseElement : element;
        baseIndex.remove(s, o, (T) ele);
    }
}
