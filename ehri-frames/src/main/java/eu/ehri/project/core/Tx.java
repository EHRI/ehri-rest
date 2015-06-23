package eu.ehri.project.core;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Tx extends AutoCloseable {
    void success();
    void failure();
    void close();
}
