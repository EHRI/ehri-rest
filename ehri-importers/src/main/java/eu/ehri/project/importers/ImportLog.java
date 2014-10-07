package eu.ehri.project.importers;

import java.util.Map;

import com.google.common.collect.Maps;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager.EventContext;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * Class that serves as a manifest for an import batch,
 * detailing how many items were created and updated,
 * and how many failed.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ImportLog {

    private int created = 0;
    private int updated = 0;
    private int unchanged = 0;
    private int errored = 0;
    private EventContext eventContext;
    private Map<String, String> errors = Maps.newHashMap();


    /**
     * Constructor.
     *
     * @param action An event context
     */
    public ImportLog(final EventContext action) {
        this.eventContext = action;
    }

    /**
     * Increment the creation count.
     */
    public void addCreated() {
        created++;
    }

    /**
     * Increment the update count.
     */
    public void addUpdated() {
        updated++;
    }

    /**
     * Increment the unchanged count.
     */
    public void addUnchanged() {
        unchanged++;
    }

    /**
     * @return returns the number of created items
     */
    public int getCreated() {
        return created;
    }

    /**
     * @return returns the number of updated items
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * @return returns the number of unchanged items
     */
    public int getUnchanged() {
        return unchanged;
    }

    /**
     * @return the number of errored item imports
     */
    public int getErrored() {
        return errored;
    }

    /**
     * @return the import errors
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * Indicate that importing the item with the given id
     * failed with the given error.
     *
     * @param item  The item
     * @param error The error that occurred
     */
    public void setErrored(String item, String error) {
        errors.put(item, error);
        errored++;
    }

    /**
     * @return returns the SystemEvent associated with this import
     */
    public SystemEvent getAction() {
        return eventContext.getSystemEvent();
    }

    /**
     * @return returns the Actioner associated with this import
     */
    public Actioner getActioner() {
        return eventContext.getActioner();
    }

    /**
     * Indicated whether the import succeeded at all,
     * in terms of items created/updated.
     *
     * @return returns whether the import succeeded
     */
    public boolean hasDoneWork() {
        return created > 0 || updated > 0;
    }

    /**
     * @return returns the number of items that were either created or updated.
     */
    public int getChanged() {
        return created + updated;
    }

    @JsonValue
    public Map<String,Integer> getData() {
        Map<String,Integer> data = Maps.newHashMap();
        data.put("created", created);
        data.put("updated", updated);
        data.put("unchanged", unchanged);
        return data;
    }

    public void printReport() {
        System.out.println(
                String.format(
                        "Created: %d, Updated: %d, Unchanged: %s",
                        created, updated, unchanged));
    }
}
