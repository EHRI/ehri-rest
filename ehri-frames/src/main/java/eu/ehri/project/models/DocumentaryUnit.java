package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.PermissionScope;

@EntityType(EntityClass.DOCUMENTARY_UNIT)
public interface DocumentaryUnit extends AccessibleEntity,
        DescribedEntity, PermissionScope {

    public static final String CHILD_OF = "childOf";

    /**
     * Get the repository that holds this documentary unit.
     * @return
     */
    @Fetch(Repository.HELD_BY)
    @GremlinGroovy("it.copySplit(_(), _().as('n').out('" + CHILD_OF +"')"
            + ".loop('n'){it.loops < 40}{!it.object.out('" + CHILD_OF +"').hasNext()}"
            + ").exhaustMerge().out('" + Repository.HELD_BY + "')")
    public Repository getRepository();

    /**
     * Set the repository that holds this documentary unit.
     * @param institution
     */
    @Adjacency(label = Repository.HELD_BY)
    public void setRepository(final Repository institution);

    /**
     * Get parent documentary unit, if any
     * @return
     */
    @Fetch(CHILD_OF)
    @Adjacency(label = CHILD_OF)
    public DocumentaryUnit getParent();

    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public void addChild(final DocumentaryUnit child);

    /*
     * Fetches a list of all ancestors (parent -> parent -> parent)
     */
    @GremlinGroovy("it.as('n').out('" + CHILD_OF
            + "').loop('n'){it.loops < 20}{true}")
    public Iterable<DocumentaryUnit> getAncestors();

    /**
     * Get child documentary units
     * @return
     */
    @Adjacency(label = DocumentaryUnit.CHILD_OF, direction = Direction.IN)
    public Iterable<DocumentaryUnit> getChildren();

    @Adjacency(label = DescribedEntity.DESCRIBES, direction = Direction.IN)
    public Iterable<DocumentDescription> getDocumentDescriptions();
}
