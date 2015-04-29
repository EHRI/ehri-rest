package eu.ehri.project.core;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Tx extends AutoCloseable {
    public void success();
    public void failure();
    public void close();
}
