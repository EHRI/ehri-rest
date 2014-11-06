package eu.ehri.project.models.idgen;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class GenericIdGeneratorTest {
    @Test
    public void testGetUUIDIsSequential() throws Exception {

        int testRuns = 1000;
        UUID[] testIds = new UUID[testRuns];
        for (int i = 0; i < testRuns; i++) {
            testIds[i] = GenericIdGenerator.getTimeBasedUUID();
        }

        for (int i = 1; i < testRuns; i++) {
            UUID last = testIds[i - 1];
            UUID current = testIds[i];
            if (!(last.compareTo(current) < 0)) {
                fail("Time-based UUID was not sequential at test run " + i);
            }
        }
    }
}
