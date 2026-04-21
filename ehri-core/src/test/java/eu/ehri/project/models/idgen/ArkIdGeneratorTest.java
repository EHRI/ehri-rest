/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.idgen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArkIdGeneratorTest {

    @Test
    public void testGenerateIdLength() {
        RandomIdGenerator gen = ArkIdGenerator.create(10);
        String id = gen.generateId();
        assertEquals(10, id.length());
    }

    @Test
    public void testGenerateIdShoulder() {
        RandomIdGenerator gen = ArkIdGenerator.create(10, "x5");
        String id = gen.generateId();
        assertEquals(12, id.length());
        assertEquals("x5", id.substring(0, 2));
    }
}