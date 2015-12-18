package eu.ehri.project.core;


public interface Tx extends AutoCloseable {
    void success();
    void failure();
    void close();
}
