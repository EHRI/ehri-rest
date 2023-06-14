package eu.ehri.project.importers.util;

import eu.ehri.project.definitions.Ontology;
import org.apache.jena.ext.com.google.common.collect.Maps;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class DateRange {

    private static final DateTimeFormatter isoDateFormat = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LocalDate start;
    private final LocalDate end;
    private final String description;

    public DateRange(LocalDate start, LocalDate end, String description) {
        if (start == null) {
            throw new IllegalArgumentException("DateRange start must not be null");
        }
        this.start = start;
        this.end = end;
        this.description = description;
    }

    public static DateRange of(LocalDate start, LocalDate end, String description) {
        return new DateRange(start, end, description);
    }

    public DateRange(LocalDate start, String description) {
        this(start, null, description);
    }

    @Override
    public String toString() {
        return end != null
                ? String.format("%s - %s", toLocalDateString(start), toLocalDateString(end))
                : toLocalDateString(start);
    }

    /**
     * Debug constructor: creates a DateRange from a "YYYY-MM-DD - YYYY-MM-DD"
     * string.
     */
    public static DateRange fromString(String s, String description) {
        final String[] split = s.split("\\s-\\s");
            final LocalDate d1 = LocalDate.parse(split[0]);
            final LocalDate d2 = split.length > 1 ? LocalDate.parse(split[1]) : null;
            return new DateRange(d1, d2, description);
    }

    public Map<String, Object> data() {
        Map<String, Object> data = Maps.newHashMap();
        data.put(Ontology.DATE_PERIOD_START_DATE, toLocalDateString(start));
        if (end != null) {
            data.put(Ontology.DATE_PERIOD_END_DATE, toLocalDateString(end));
        }
        if (description != null) {
            data.put(Ontology.DATE_HAS_DESCRIPTION, description);
        }
        return data;
    }

    private String toLocalDateString(LocalDate instant) {
        return instant.format(isoDateFormat);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return start.equals(dateRange.start)
                && Objects.equals(end, dateRange.end)
                && description.equals(dateRange.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, description);
    }
}
