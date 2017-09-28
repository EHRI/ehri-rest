package eu.ehri.project.importers.base;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ErrorCallback;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.models.base.Accessible;

import java.util.List;


/**
 * Interface for importers that import documentary units, historical agents and virtual collections,
 * with their constituent logical data, description(s), and date periods.
 *
 * @param <I> Type of node representation that can be imported,
 *            for example, {@code Map<String, Object>}.
 */
public interface ItemImporter<I, T extends Accessible> {
    /**
     * Import an item representation into the graph, and return the Node.
     *
     * @param itemData the item representation to import
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    T importItem(I itemData) throws ValidationError;

    /**
     * Import an item representation into the graph at a certain depth, and return the Node.
     *
     * @param itemData the item representation to import
     * @param scopeIds parent identifiers for ID generation,
     *                 not including permission scope
     * @return the imported node
     * @throws ValidationError when the item representation does not validate
     */
    T importItem(I itemData, List<String> scopeIds) throws ValidationError;

    /**
     * Used to indicate a validation error in the
     * handler for a given element.
     *
     * @param ex      a validation exception
     */
    void handleError(Exception ex);

    /**
     * Add a callback to run when an item is created.
     *
     * @param callback a callback function object
     */
    void addCallback(ImportCallback callback);

    /**
     * Add a callback to run when an item errors.
     *
     * @param callback a callback function object
     */
    void addErrorCallback(ErrorCallback callback);
}
