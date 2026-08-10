/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
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

package eu.ehri.project.importers.ead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import eu.ehri.project.importers.ImportLog;

import java.util.Map;
import java.util.Set;

public class SyncLog {
    private final ImportLog log;
    private final Set<String> deleted;
    private final Map<String, String> moved;
    private final Set<String> created;

    @JsonCreator
    public SyncLog(
            @JsonProperty("log") ImportLog log,
            @JsonProperty("created") Set<String> created, @JsonProperty("deleted") Set<String> deleted,
            @JsonProperty("moved") Map<String, String> moved) {
        this.log = log;
        this.deleted = deleted;
        this.moved = moved;
        this.created = created;
    }

    @JsonValue
    public Map<String, Object> getData() {
        return ImmutableMap.of(
            "log", log,
            "created", created,
            "deleted", deleted,
            "moved", moved
        );
    }

    public ImportLog log() {
        return log;
    }


    public Set<String> deleted() {
        return deleted;
    }


    public Set<String> created() {
        return created;
    }


    public Map<String, String> moved() {
        return moved;
    }

    @Override
    public String toString() {
        return String.format("%s, Moved: %d", log.toString(), moved.size());
    }
}
