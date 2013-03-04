package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Revision;

public interface VersionedEntity extends VertexFrame {
    public static final String HAS_REVISION = "hasRevision";

    @Adjacency(label = HAS_REVISION)
    public Iterable<Revision> getRevisions();
}
