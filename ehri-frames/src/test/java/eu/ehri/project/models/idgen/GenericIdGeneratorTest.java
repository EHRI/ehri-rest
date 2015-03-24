/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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
