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

package eu.ehri.project.importers.csv;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to convert Terezin data.
 */
public class TerezinDataConverter {

    private static final Logger logger = LoggerFactory.getLogger(TerezinDataConverter.class);

    public static void readFile(InputStream stream) {
        Scanner scanner = new Scanner(stream);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] quotes = line.split("\"");
            String[] values;
            if (quotes.length == 1) {
                values = line.split(";");
            } else {
                if (quotes[0].length() <= 1) {
                    logger.error("Problem found at line: {}: {}", line, quotes[0]);
                    break;
                }
                values = new String[2];
                values[0] = quotes[0].substring(0, quotes[0].length() - 2);
                values[1] = quotes[1];
            }
            if (values.length == 2) {
                String[] datevalues = values[1].split(";");
                for (String datevalue : datevalues) {
                    List<Calendar> list = parseDate(datevalue);
                    if (list == null) {
                        //parse failed, print the date and the identifier
                        logger.error(values[0] + " := " + datevalue);
                    }
                }
            } else {
                logger.error(values[0] + " has a problem: " + line);
            }
        }
        scanner.close();
    }

    public static List<Calendar> parseDate(String datevalue) {
        String trimmedDate = datevalue.trim();
        List<Calendar> dates = Lists.newArrayList();
        dates.add(Calendar.getInstance());

        if (trimmedDate.startsWith("before ")) {
            dates = parseDate(trimmedDate.substring(6));
            if (dates == null)
                return null;

            dates.add(0, Calendar.getInstance());
            dates.get(0).set(Calendar.YEAR, 1900);
            dates.get(0).set(Calendar.DATE, 1);
            dates.get(0).set(Calendar.MONTH, Calendar.JANUARY);
            return dates;
        }
        if (trimmedDate.startsWith("after ")) {
            dates = parseDate(trimmedDate.substring(5));
            if (dates == null)
                return null;
            dates.get(0).set(Calendar.YEAR, dates.get(0).get(Calendar.YEAR) + 1);
            dates.add(1, Calendar.getInstance());
            return dates;
        }

        // April 1942
        //July 1946
        ParsePosition p = new ParsePosition(0);
        SimpleDateFormat monthDateFormat = new SimpleDateFormat("MMM yyyy", Locale.US);
        Date d = monthDateFormat.parse(trimmedDate, p);
        if (p.getIndex() > 0) {
            dates.get(0).setTime(d);
            dates.add(Calendar.getInstance());
            dates.get(1).setTime(d);

            dates.get(0).set(Calendar.DATE, 1);

            dates.get(1).set(Calendar.MONTH, dates.get(1).get(Calendar.MONTH) + 1);
            dates.get(1).set(Calendar.DATE, 0);
            return dates;
        }


        Matcher m;
        //1941 - June 1943
        if ((m = parseDate(trimmedDate, "(\\d{4})\\s*-\\s*(\\D+\\s+\\d{4})")) != null) {
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, Calendar.JANUARY);
            dates.get(0).set(Calendar.DATE, 1);

            dates.add(Calendar.getInstance());
            d = monthDateFormat.parse(m.group(2), p);
            if (p.getIndex() > 0) {
                dates.get(1).setTime(d);
                dates.get(1).set(Calendar.MONTH, dates.get(1).get(Calendar.MONTH) + 1);
                dates.get(1).set(Calendar.DATE, 0);
            }

            return dates;
        }
        //May - June 1945
        if ((m = parseDate(trimmedDate, "(\\D*)\\s*-\\s*(\\D+\\s+\\d{4})")) != null) {

            SimpleDateFormat month = new SimpleDateFormat("MMM", Locale.US);
            d = month.parse(m.group(1), p);
            if (p.getIndex() > 0) {
                dates.get(0).setTime(d);
                dates.get(0).set(Calendar.DATE, 1);
            }

            dates.add(Calendar.getInstance());
            p.setIndex(0);
            d = monthDateFormat.parse(m.group(2), p);
            if (p.getIndex() > 0) {
                dates.get(1).setTime(d);
                dates.get(1).set(Calendar.MONTH, dates.get(1).get(Calendar.MONTH) + 1);
                dates.get(1).set(Calendar.DATE, 0);
                dates.get(0).set(Calendar.YEAR, dates.get(1).get(Calendar.YEAR));
            }

            return dates;
        }

        //1.11.1943-10.11.1943
        if ((m = parseDate(trimmedDate, "(\\d+)\\.(\\d+)\\.(\\d{4}).*?-.*?(\\d+)\\.(\\d+)\\.(\\d{4})")) != null) {
            dates.get(0).set(Calendar.DATE, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, new Integer(m.group(2)) - 1); //Calendar.month counts from 0 ... 11
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(3)));

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.DATE, new Integer(m.group(4)));
            dates.get(1).set(Calendar.MONTH, new Integer(m.group(5)) - 1);
            dates.get(1).set(Calendar.YEAR, new Integer(m.group(6)));

            return dates;
        }
        //1.12. - 24.12.1942
        if ((m = parseDate(trimmedDate, "(\\d+)\\.(\\d+)\\..*?-.*?(\\d*)\\.(\\d*)\\.(\\d{4})")) != null) {
            dates.get(0).set(Calendar.DATE, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, new Integer(m.group(2)) - 1); //Calendar.month counts from 0 ... 11
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(5)));

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.DATE, new Integer(m.group(3)));
            dates.get(1).set(Calendar.MONTH, new Integer(m.group(4)) - 1);
            dates.get(1).set(Calendar.YEAR, new Integer(m.group(5)));

            return dates;
        }
        // 1. - 30.11.1942
        if ((m = parseDate(trimmedDate, "(\\d+)\\.\\s*?-\\s*?(\\d*)\\.(\\d*)\\.(\\d{4})")) != null) {
            dates.get(0).set(Calendar.DATE, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, new Integer(m.group(3)) - 1); //Calendar.month counts from 0 ... 11
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(4)));

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.DATE, new Integer(m.group(2)));
            dates.get(1).set(Calendar.MONTH, new Integer(m.group(3)) - 1);
            dates.get(1).set(Calendar.YEAR, new Integer(m.group(4)));

            return dates;
        }
        // 1940 - 1942
        if ((m = parseDate(trimmedDate, "(\\d{4})\\s*-\\s*(\\d{4})")) != null) {
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, Calendar.JANUARY);
            dates.get(0).set(Calendar.DATE, 1);

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.YEAR, new Integer(m.group(2)));
            dates.get(1).set(Calendar.MONTH, Calendar.DECEMBER);
            dates.get(1).set(Calendar.DATE, 31);

            return dates;
        }
        // 1940 - 42
        if ((m = parseDate(trimmedDate, "(\\d{4})\\s*-\\s*(\\d{2})")) != null) {
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, Calendar.JANUARY);
            dates.get(0).set(Calendar.DATE, 1);

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.YEAR, 1900 + new Integer(m.group(2)));
            dates.get(1).set(Calendar.MONTH, Calendar.DECEMBER);
            dates.get(1).set(Calendar.DATE, 31);

            return dates;
        }
        //20.3.1942
        if ((m = parseDate(trimmedDate, "(\\d*)\\.(\\d*)\\.(\\d{4})")) != null) {
            dates.get(0).set(Calendar.DATE, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, new Integer(m.group(2)) - 1);
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(3)));
            return dates;
        }
        //05.03.43
        if ((m = parseDate(trimmedDate, "(\\d*)\\.(\\d*)\\.(\\d{2})")) != null) {
            dates.get(0).set(Calendar.DATE, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, new Integer(m.group(2)) - 1);
            dates.get(0).set(Calendar.YEAR, 1900 + new Integer(m.group(3)));
            return dates;
        }
        //1942
        if ((m = parseDate(trimmedDate, "(\\d{4})")) != null) {
            dates.get(0).set(Calendar.YEAR, new Integer(m.group(1)));
            dates.get(0).set(Calendar.MONTH, Calendar.JANUARY);
            dates.get(0).set(Calendar.DATE, 1);

            dates.add(Calendar.getInstance());
            dates.get(1).set(Calendar.YEAR, new Integer(m.group(1)));
            dates.get(1).set(Calendar.MONTH, Calendar.DECEMBER);
            dates.get(1).set(Calendar.DATE, 31);

            return dates;
        }
        return null;
    }

    private static Matcher parseDate(String datevalue, String pattern) {
        Pattern yearPattern = Pattern.compile(pattern);
        Matcher matcher = yearPattern.matcher(datevalue);
        if (matcher.matches()) {
            return matcher;
        }
        return null;
    }
}
