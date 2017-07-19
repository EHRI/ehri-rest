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
        return getData().toString();
    }
}
