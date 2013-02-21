package eu.ehri.project.importers;

import eu.ehri.project.importers.old.AbstractImporter;
import java.util.List;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;

/**
 * Abstract base class for importers that must descend a tree like structure
 * such as EAD, where elements contain multiple child items.
 * 
 * Mea culpa: it's likely that this will only be useful for EAD.
 * 
 * This abstract class handles importing documents of type T, which contain one
 * or more logical documentary units, which may themselves contain multiple
 * child items.
 * 
 * As expressed in EAD, this looks like:
 * 
 * ead archdesc <- logical item
 * 
 * or
 * 
 * ead archdesc dsc c01 <- logical item c01 <- logical item 2 c02 <- child of
 * item 2
 * 
 * This abstract class handles the top-level logic of importing this type of
 * structure and persisting the data. Implementing concrete classes must
 * implement:
 * 
 * 1. Extracting from document T the entry points for each top-level and
 * returning an iterable set of item nodes T1.
 * 
 * 2. Extracting from item node T1 an iterable set of child nodes T2.
 * 
 * 3. Extracting the item data from node T1 (logical unit, descriptions, dates).
 * 
 * The data type T is, in the EAD example, an XML Node, but this depends on the
 * implementing class (it could just as well be a JsonTree object.) The importer
 * is initialised using the full document (i.e. the &lt;ead&gt; node), and
 * logical item data extraction is performed on &lt;archdesc&gt; or &lt;c0X&gt;
 * nodes.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public abstract class AbstractMultiItemRecursiveImporter<T> extends
        AbstractImporter<T> {
    protected Boolean tolerant = false;

    /**
     * Constructor.
     * 
     * @param framedGraph
     * @param repository
     * @param log
     * @param documentContext
     */
    public AbstractMultiItemRecursiveImporter(
            FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            ImportLog log, T documentContext) {
        super(framedGraph, repository, log, documentContext);
    }

    /**
     * Tell the importer to simply skip invalid items rather than throwing an
     * exception.
     * 
     * @param tolerant
     */
    public void setTolerant(Boolean tolerant) {
        this.tolerant = tolerant;
    }

    /**
     * Extract child items from an item node.
     * 
     * @param itemData
     * @return
     */
    protected abstract List<T> extractChildItems(T itemData);

    /**
     * Get the entry point to the top level item data.
     * 
     * @param topLevelData
     */
    protected abstract Iterable<T> getEntryPoints() throws ValidationError,
            InvalidInputFormatError;

    /**
     * Top-level entry point for importing some EAD.
     * 
     * @throws ValidationError
     * @throws InvalidInputFormatError
     * @throws IntegrityError 
     * 
     */
    public void importItems() throws ValidationError, InvalidInputFormatError, IntegrityError {
        for (T item : getEntryPoints()) {
            importItem(item, null, 0);
        }
    }

    /**
     * Import an item, and them recursively import its children.
     * 
     * @param itemData
     * @param parent
     * @param depth
     * @throws ValidationError
     * @throws IntegrityError 
     */
    @Override
    public DocumentaryUnit importItem(T itemData, DocumentaryUnit parent,
            int depth) throws ValidationError, IntegrityError {

        DocumentaryUnit frame = super.importItem(itemData, parent, depth);

        // Search through child parts and add them recursively...
        for (T child : extractChildItems(itemData)) {
            try {
                importItem(child, frame, depth + 1);
            } catch (ValidationError e) {
                // TODO: Improve error context.
                log.setErrored("Item at depth: " + depth, e.getMessage());
                if (!tolerant) {
                    throw e;
                }
            }
        }

        return frame;
    }
}