package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.ElementHelper;
import eu.ehri.project.models.base.Accessor;

import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AclElement implements Element {
    protected Element baseElement;
    protected AclGraph<?> graph;

    protected AclElement(final Element baseElement, final AclGraph<?> graph) {
        this.baseElement = baseElement;
        this.graph = graph;
    }

    @Override
    public <T> T getProperty(String s) {
        return baseElement.getProperty(s);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return baseElement.getPropertyKeys();
    }

    @Override
    public void setProperty(String s, Object o) {
        baseElement.setProperty(s, o);
    }

    @Override
    public <T> T removeProperty(String s) {
        return baseElement.removeProperty(s);
    }

    @Override
    public void remove() {
        baseElement.remove();
    }

    @Override
    public Object getId() {
        return baseElement.getId();
    }

    @Override
    public String toString() {
        return "[" + getId() + " (" + graph.getAccessor().getId() + ")]";
    }

    @Override
    public boolean equals(final Object object) {
        return ElementHelper.areEqual(this, object);
    }

    @Override
    public int hashCode() {
        // NB: Deliberate decision to ignore
        // accessor when calculating hashCode
        // or equality.
        return baseElement.hashCode();
    }

    public Element getBaseElement() {
        return baseElement;
    }

    public Accessor getAccessor() {
        return graph.getAccessor();
    }
}
