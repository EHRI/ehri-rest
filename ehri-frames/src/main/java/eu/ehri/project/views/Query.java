package eu.ehri.project.views;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jIndex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.groovy.GremlinGroovyPipeline;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Converter;

public class Query<E extends AccessibleEntity> implements IQuery<E> {
    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;

    /**
     * @param graph
     * @param cls
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }

    /**
     * List items accessible to a given user.
     * 
     * @param user
     * @return
     */
    public Iterable<E> list(long user) {
        final HashSet<Object> all = getAllAccessors(user);

        //Index<Vertex> index = graph.getBaseGraph().getIndex(getEntityIndexName(cls), Vertex.class);
        //Iterator<Vertex> v = index.query("*", "*");
        
        GraphDatabaseService rawGraph = graph.getBaseGraph().getRawGraph();
        Index<Vertex> index = graph.getBaseGraph().getIndex(getEntityIndexName(cls), Vertex.class);
        
        
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GremlinPipeline<?, Vertex> filter = new GremlinPipeline()
             //   .setStarts()
                .filter(new PipeFunction<Vertex, Boolean>() {
                    public Boolean compute(Vertex v) {
                        Iterable<Edge> edges = v.getEdges(Direction.OUT, AccessibleEntity.ACCESS);
                        // If there's no Access conditions, it's read-only...
                        if (!edges.iterator().hasNext())
                            return true;
                        for (Edge e : edges) {
                            // FIXME: Does not currently check the
                            // actual permission property. This assumes
                            // that if there's a permission, it means that
                            // the subject can be read.                            
                            Vertex other = e.getVertex(Direction.IN);
                            if (all.contains(other.getId()))
                                return true;                            
                        }
                        return false;
                    }
                });
        
        return graph.frameVertices(filter, cls);
    }

    /**
     * For a given user, fetch a lookup of all the inherited
     * accessors it belongs to.
     * 
     * @param user
     * @return
     */
    private HashSet<Object> getAllAccessors(long user) {
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Iterable<Accessor> parents = accessor.getAllParents();
        final HashSet<Object> all = new HashSet<Object>();
        for (Accessor a : parents)
            all.add(a.asVertex().getId());
        all.add(accessor.asVertex().getId());
        return all;
    }
    
    private String getEntityIndexName(Class<E> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann != null) return ann.value();
        return null;        
    }
}
