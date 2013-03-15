package eu.ehri.project.test.utils.fixtures;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for classes which handle fixture loading.
 * 
 * @author mike
 *
 */
public interface FixtureLoader {

    /**
     * Load the default fixtures.
     */
    public void loadTestData();

    /**
     * Load a given InputStream as test data. The stream
     * will be closed automatically.
     * @param inputStream
     */
    public void loadTestData(InputStream inputStream);

    /**
     * Load a given file as test data.
     *
     * @param filePath
     */
    public void loadTestData(String filePath);
}
