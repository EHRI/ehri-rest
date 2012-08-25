package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.relationships.Access;
import eu.ehri.project.relationships.RevisionContext;

public interface VersionedEntity extends VertexFrame {
    public static final String HAS_REVISION = "hasRevision";
    
    @Adjacency(label=HAS_REVISION)
    public Iterable<Revision> getRevisions();
    
    @Incidence(label = HAS_REVISION)
    public Iterable<RevisionContext> getRevisionContexts();

}
