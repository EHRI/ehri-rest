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

package eu.ehri.project;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.models.idgen.ArkIdGenerator;
import eu.ehri.project.models.idgen.RandomIdGenerator;

public class IdGeneratorProvider {
    private static final Config config = ConfigFactory.load();
    private static final int length = config.getInt("io.pids.length");

    public static RandomIdGenerator getIdGenerator() {
        return new ArkIdGenerator(length);
    }
}
