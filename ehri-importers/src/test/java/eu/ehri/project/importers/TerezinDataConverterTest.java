/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class TerezinDataConverterTest {

    public TerezinDataConverterTest() {
    }

//    @Test
    public void testReadFile() {
        InputStream ios = ClassLoader.getSystemResourceAsStream("terezin_dates_better.csv");
        TerezinDataConverter.readFile(ios);
    }

    //1942
    @Test
    public void testDateParserYear() {
        List<Calendar> d = TerezinDataConverter.parseDate("1942");
        assertEquals(1942, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        assertEquals(1942, d.get(1).get(Calendar.YEAR));
        assertEquals(31, d.get(1).get(Calendar.DATE));
        assertEquals(11, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(1).get(Calendar.MONTH));
    }

    // 20.3.1942
    //05.03.43
    @Test
    public void testDateParserDayMonthYear() {
        List<Calendar> d = TerezinDataConverter.parseDate(" 20.3.1942");
        assertEquals(1942, d.get(0).get(Calendar.YEAR));
        assertEquals(20, d.get(0).get(Calendar.DATE));
        assertEquals(2, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.MARCH, d.get(0).get(Calendar.MONTH));

        d = TerezinDataConverter.parseDate("05.03.43");
        assertEquals(5, d.get(0).get(Calendar.DATE));
        assertEquals(2, d.get(0).get(Calendar.MONTH));
        assertEquals(1943, d.get(0).get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, d.get(0).get(Calendar.MONTH));
}

    //1.12. - 24.12.1942
    @Test
    public void testDateParserPeriod() {
        List<Calendar> d = TerezinDataConverter.parseDate("1.12. - 24.12.1942");
        assertEquals(1942, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(11, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(0).get(Calendar.MONTH));
        assertEquals(1942, d.get(1).get(Calendar.YEAR));
        assertEquals(24, d.get(1).get(Calendar.DATE));
        assertEquals(11, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(1).get(Calendar.MONTH));
    }

    //1.11.1943-10.11.1943
    @Test
    public void testDateParserPeriodWithTwoDateMonthYears() {
        List<Calendar> d = TerezinDataConverter.parseDate("1.11.1943-10.11.1943");
        assertEquals(1943, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(10, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.NOVEMBER, d.get(0).get(Calendar.MONTH));
        assertEquals(1943, d.get(1).get(Calendar.YEAR));
        assertEquals(10, d.get(1).get(Calendar.DATE));
        assertEquals(10, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.NOVEMBER, d.get(1).get(Calendar.MONTH));
    }

    //1943 - 1945
    @Test
    public void testDateParserPeriodWithZeroDateMonthTwoYears() {
        List<Calendar> d = TerezinDataConverter.parseDate("1943 - 1945");
        assertEquals(1943, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        assertEquals(31, d.get(1).get(Calendar.DATE));
        assertEquals(11, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(1).get(Calendar.MONTH));
        assertEquals(1945, d.get(1).get(Calendar.YEAR));
    }

    //1. - 30.11.1942
    @Test
    public void testDateParserPeriodWithOneDateTwoYears() {
        List<Calendar> d = TerezinDataConverter.parseDate("1. - 30.11.1942");
        assertEquals(1942, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(10, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.NOVEMBER, d.get(0).get(Calendar.MONTH));
        assertEquals(1942, d.get(1).get(Calendar.YEAR));
        assertEquals(30, d.get(1).get(Calendar.DATE));
        assertEquals(10, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.NOVEMBER, d.get(1).get(Calendar.MONTH));
    }

    //April 1945
    //July 1946
    @Test
    public void testDateParserMonthYear() {
        List<Calendar> d = TerezinDataConverter.parseDate("April 1945");
        assertEquals(1945, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(3, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.APRIL, d.get(0).get(Calendar.MONTH));

        assertEquals(1945, d.get(1).get(Calendar.YEAR));
        assertEquals(30, d.get(1).get(Calendar.DATE));
        assertEquals(3, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.APRIL, d.get(1).get(Calendar.MONTH));

        d = TerezinDataConverter.parseDate("July 1946");
        assertEquals(1946, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(6, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JULY, d.get(0).get(Calendar.MONTH));

        assertEquals(1946, d.get(1).get(Calendar.YEAR));
        assertEquals(31, d.get(1).get(Calendar.DATE));
        assertEquals(6, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.JULY, d.get(1).get(Calendar.MONTH));

    }
    
    //May - June 1945
    @Test
    public void testDateParserMonthMonthYear() {
        List<Calendar> d = TerezinDataConverter.parseDate("May - June 1945");
        assertEquals(1945, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(4, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.MAY, d.get(0).get(Calendar.MONTH));

        assertEquals(1945, d.get(1).get(Calendar.YEAR));
        assertEquals(30, d.get(1).get(Calendar.DATE));
        assertEquals(5, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.JUNE, d.get(1).get(Calendar.MONTH));
    }
    
    //1941 - June 1943
    @Test
    public void testDateParserYearMonthYear() {
        List<Calendar> d = TerezinDataConverter.parseDate("1941 - June 1943");
        assertEquals(1941, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        assertEquals(1943, d.get(1).get(Calendar.YEAR));
        assertEquals(30, d.get(1).get(Calendar.DATE));
        assertEquals(5, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.JUNE, d.get(1).get(Calendar.MONTH));
    }
    
    //before 18.12.1943
    @Test
    public void testDateParserBeforeDate() {
        List<Calendar> d = TerezinDataConverter.parseDate("before 18.12.1943");
        assertEquals(1900, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        assertEquals(1943, d.get(1).get(Calendar.YEAR));
        assertEquals(18, d.get(1).get(Calendar.DATE));
        assertEquals(11, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(1).get(Calendar.MONTH));
    }

    //after 1945
    @Test
    public void testDateParserAfterDate() {
        List<Calendar> d = TerezinDataConverter.parseDate("after 1945");
        assertEquals(1946, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        Calendar rightNow = Calendar.getInstance();
        assertEquals(rightNow.get(Calendar.YEAR), d.get(1).get(Calendar.YEAR));
        assertEquals(rightNow.get(Calendar.DATE), d.get(1).get(Calendar.DATE));
        assertEquals(rightNow.get(Calendar.MONTH), d.get(1).get(Calendar.MONTH));
    }

    //1941 - 45
    @Test
    public void testDateParserPeriodWithZeroDateMonthTwoYearsNoPrefix() {
        List<Calendar> d = TerezinDataConverter.parseDate("1941 - 45");
        assertEquals(1941, d.get(0).get(Calendar.YEAR));
        assertEquals(1, d.get(0).get(Calendar.DATE));
        assertEquals(0, d.get(0).get(Calendar.MONTH));
        assertEquals(Calendar.JANUARY, d.get(0).get(Calendar.MONTH));

        assertEquals(31, d.get(1).get(Calendar.DATE));
        assertEquals(11, d.get(1).get(Calendar.MONTH));
        assertEquals(Calendar.DECEMBER, d.get(1).get(Calendar.MONTH));
        assertEquals(1945, d.get(1).get(Calendar.YEAR));
    }
    
    //16011
    @Test
    public void testDateParserNumber() {
        List<Calendar> d = TerezinDataConverter.parseDate("16011");
        assertNull(d);
    }
    //before deportation
    @Test
    public void testDateParserText() {
        List<Calendar> d = TerezinDataConverter.parseDate("before deportation");
        assertNull(d);
    }

    //jaro 1942
    //1945 (after the liberation)
    //winter 1941
    //spring 1941
    //September - 20.11.1944
    
}
