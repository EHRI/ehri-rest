package eu.ehri.project.utils.fixtures;

import java.io.InputStream;

/**
 * Interface for classes which handle fixture loading.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public interface FixtureLoader {

    /**
     * Toggle whether or not initialization occurs before
     * loading (default: true)
     */
    public FixtureLoader setInitializing(boolean toggle);

    /**
     * Load the default fixtures.
     */
    public void loadTestData();

    /**
     * Load a given InputStream as test data. The stream
     * will be closed automatically.
     * @param inputStream An imput stream of fixture data
     */
    public void loadTestData(InputStream inputStream);

    /**
     * Load a given file as test data.
     *
     * @param resourceNameOrPath A resource name or file path
     *                           containing fixture data.
     */
    public void loadTestData(String resourceNameOrPath);
}
