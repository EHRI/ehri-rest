package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.Map;

import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager.EventContext;

/**
 * Class that serves as a manifest for an import batch,
 * detailing how many items were created and updated,
 * and how many failed.
 * 
 * @author mike
 *
 */
public class ImportLog {

	private int created = 0;
	private int updated = 0;
	private int errored = 0;
	private EventContext eventContext;
	private Map<String, String> errors = new HashMap<String, String>();
	
	
	/**
	 * Constructor.
	 * 
	 * @param action
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
	 * 
	 */
	public void addUpdated() {
		updated++;
	}
	
	/**
         * 
	 * @return returns the number of created items
	 */
	public int getCreated() {
		return created;
	}

	/**
	 * 
	 * @return returns the number of updated items
	 */
	public int getUpdated() {
		return updated;
	}

	/**
         * 
	 * @return returns the number of errored item imports
	 */
	public int getErrored() {
		return errored;
	}

	/**
	 * 
	 * @return returns the import errors
	 */
	public Map<String, String> getErrors() {
		return errors;
	}
	
	/**
	 * Indicate that importing the item with the given id
	 * failed with the given error.
	 * 
	 * @param item
	 * @param error
	 */
	public void setErrored(String item, String error) {
		errors.put(item, error);
		errored++;
	}
	
	/**
	 * 
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
	public boolean isValid() {
		return created > 0 || updated > 0;
	}

	/**
	 * 
	 * @return returns the number of items that were either created or updated.
	 */
	public int getSuccessful() {
		return created + updated;
	}
}
