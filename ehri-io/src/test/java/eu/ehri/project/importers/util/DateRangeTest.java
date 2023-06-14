package eu.ehri.project.importers.util;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class DateRangeTest {

    @Test
    public void fromString() {
        assertEquals(DateRange.fromString("2020-01-01", ""),
                new DateRange(LocalDate.parse("2020-01-01"), ""));
    }
}