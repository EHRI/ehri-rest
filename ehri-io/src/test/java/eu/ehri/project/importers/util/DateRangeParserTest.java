package eu.ehri.project.importers.util;

import com.google.common.collect.ImmutableList;
import org.apache.commons.compress.utils.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class DateRangeParserTest {

    private DateRangeParser rangeParser;

    @Before
    public void setUp() throws Exception {
        rangeParser = new DateRangeParser();
    }

    @Test
    public void parse() {
        check("1939 ca.", "1939-01-01 - 1939-12-31");
        check("1939 - 1942", "1939-01-01 - 1942-12-31");
        check("01/1942 - 06/1942", "1942-01-01 - 1942-06-30");
        check("1935-03/1935-05", "1935-03-01 - 1935-05-31");
        check("summer 1940", "1940-01-01 - 1940-12-31"); // Meh? good enough
        check("1939-01-01 - 1942-12-31", "1939-01-01 - 1942-12-31");
        check("1935-03-03 - 1945-05", "1935-03-03 - 1945-05-31");
        check("1935-03 - 1945-05-01", "1935-03-01 - 1945-05-01");
    }

    @Test
    public void parseInvalid() {
        checkNone("blob");
        checkNone("40");
        checkNone("13/1942");
        checkNone("mid-1940"); // TODO: support this?
        checkNone("1939-06-31");
    }

    private void check(String sloppy, String canonical) {
        assertEquals(DateRange.fromString(canonical, sloppy), rangeParser.parse(sloppy));
    }

    private void checkNone(String sloppy) {
        assertEquals(Optional.empty(), rangeParser.tryParse(sloppy));
    }
}