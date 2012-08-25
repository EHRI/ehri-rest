package eu.ehri.project.relationships;

import com.tinkerpop.frames.Domain;
import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.Range;

import eu.ehri.project.models.base.Revision;
import eu.ehri.project.models.base.VersionedEntity;

public interface RevisionContext extends EdgeFrame {
    @Range
    public Revision getRevision();

    @Domain
    public VersionedEntity getEntity();
}
