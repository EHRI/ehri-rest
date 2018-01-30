package eu.ehri.project.models.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.ClassUtilities;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.MethodHandler;
import com.tinkerpop.frames.structures.FramedVertexIterable;
import eu.ehri.project.models.annotations.UniqueAdjacency;

import java.lang.reflect.Method;

public class UniqueAdjacencyAnnotationHandler implements MethodHandler<UniqueAdjacency> {

    @Override
    public Class<UniqueAdjacency> getAnnotationType() {
        return UniqueAdjacency.class;
    }

    @Override
    public Object processElement(final Object frame, final Method method, final Object[] arguments, final UniqueAdjacency annotation, final FramedGraph<?> framedGraph,
            final Element element) {
        if (element instanceof Vertex) {
            return processVertex(annotation, method, arguments, framedGraph, (Vertex) element);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public Object processVertex(final UniqueAdjacency adjacency, final Method method, final Object[] arguments, final FramedGraph<?> framedGraph, final Vertex vertex) {
        Class<?> returnType = method.getReturnType();
        if (method.getName().startsWith("count")) {
            return Iterables.size(vertex.getVertices(adjacency.direction(), adjacency.label()));
        } else if (ClassUtilities.isGetMethod(method)) {
            if (Iterable.class.isAssignableFrom(returnType)) {
                return new FramedVertexIterable<Vertex>(
                        framedGraph, vertex.getVertices(adjacency.direction(), adjacency.label()),
                        ClassUtilities.getGenericClass(method));
            } else if (method.getName().startsWith("is")
                    && (returnType.equals(Boolean.TYPE) || returnType.equals(Boolean.class))) {
                // If there's an argument, assume it's another item
                // and we're checking if they're related...
                if (arguments == null) {
                    return vertex.getVertices(adjacency.direction(), adjacency.label()).iterator().hasNext();
                } else {
                    Vertex other = ((VertexFrame) arguments[0]).asVertex();
                    return adjacency.direction() == Direction.OUT
                            ? JavaHandlerUtils.hasRelationship(vertex, other, adjacency.label())
                            : JavaHandlerUtils.hasRelationship(other, vertex, adjacency.label());
                }
            } else {
                Iterable<Vertex> vertices = vertex.getVertices(adjacency.direction(), adjacency.label());
                return vertices.iterator().hasNext()
                        ? framedGraph.frame(vertices.iterator().next(),
                                ClassUtilities.getGenericClass(method))
                        : null;
            }
        } else if (ClassUtilities.isAddMethod(method)) {
            Vertex newVertex;
            Object returnValue = null;
            if (arguments == null) {
                // Use this method to get the vertex so that the vertex
                // initializer is called.
                returnValue = framedGraph.addVertex(null, returnType);
                newVertex = ((VertexFrame) returnValue).asVertex();
            } else {
                newVertex = ((VertexFrame) arguments[0]).asVertex();
            }
            addEdges(adjacency, vertex, newVertex);

            if (returnType.isPrimitive()) {
                return null;
            } else {
                return returnValue;
            }

        } else if (ClassUtilities.isRemoveMethod(method)) {
            removeEdges(adjacency.direction(), adjacency.label(), vertex, ((VertexFrame) arguments[0]).asVertex(), framedGraph);
            return null;
        } else if (ClassUtilities.isSetMethod(method)) {
            removeEdges(adjacency.direction(), adjacency.label(), vertex, null, framedGraph);
            if (ClassUtilities.acceptsIterable(method)) {
                for (Object o : (Iterable<?>) arguments[0]) {
                    Vertex v = ((VertexFrame) o).asVertex();
                    addEdges(adjacency, vertex, v);
                }
                return null;
            } else {
                if (null != arguments[0]) {
                    Vertex newVertex = ((VertexFrame) arguments[0]).asVertex();
                    addEdges(adjacency, vertex, newVertex);
                }
                return null;
            }
        }

        return null;
    }

    private void addEdges(final UniqueAdjacency adjacency, final Vertex vertex, Vertex newVertex) {
        switch(adjacency.direction()) {
            case OUT:
                if (adjacency.single()) {
                    JavaHandlerUtils.addSingleRelationship(vertex, newVertex, adjacency.label());
                } else {
                    JavaHandlerUtils.addUniqueRelationship(vertex, newVertex, adjacency.label());
                }
                break;
            case IN:
                if (adjacency.single()) {
                    JavaHandlerUtils.addSingleRelationship(newVertex, vertex, adjacency.label());
                } else {
                    JavaHandlerUtils.addUniqueRelationship(newVertex, vertex, adjacency.label());
                }
                break;
            case BOTH:
                throw new UnsupportedOperationException("Direction.BOTH it not supported on 'add' or 'set' methods");
        }
    }

    private void removeEdges(final Direction direction, final String label, final Vertex element, final Vertex otherVertex, final FramedGraph<?> framedGraph) {
        for (final Edge edge : element.getEdges(direction, label)) {
            if (null == otherVertex || edge.getVertex(direction.opposite()).equals(otherVertex)) {
                framedGraph.removeEdge(edge);
            }
        }
    }
}