package eu.ehri.project.exceptions;

/**
 * Represents a failure to find an item in the graph based
 * on its ID value or an arbitrary key/value pair.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ItemNotFound extends Exception {
    private static final long serialVersionUID = -3562833443079995695L;

    private String key;
    private String value;

    public ItemNotFound(String id) {
        super(String.format("Item with id '%s' not found", id));
        this.key = "id";
        this.value = id;
    }

    public ItemNotFound(String key, String value) {
        super(String.format("Item with key '%s'='%s' not found", key, value));
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
