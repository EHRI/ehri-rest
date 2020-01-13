/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.oaipmh;

import java.time.ZonedDateTime;
import java.util.List;

class OaiPmhDeleted {

    private final String id;
    private final ZonedDateTime datestamp;
    private final List<String> sets;

    OaiPmhDeleted(String id, ZonedDateTime datestamp, List<String> sets) {
        this.id = id;
        this.datestamp = datestamp;
        this.sets = sets;
    }

    String getId() {
        return id;
    }

    ZonedDateTime getDatestamp() {
        return datestamp;
    }

    List<String> getSets() {
        return sets;
    }
}
