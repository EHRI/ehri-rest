package eu.ehri.project.importers.util;

import com.github.sisyphsu.dateparser.DateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateRangeParser {

    private static final Logger logger = LoggerFactory.getLogger(DateRangeParser.class);
    private static final Pattern yearRange = Pattern.compile("^(?<start>\\d{4})\\s?[\\-/]\\s?(?<end>\\d{4})$");
    private static final String SEP_CHARS = "/-";

    private final DateParser parser;

    public DateRangeParser() {
        parser = DateParser.newBuilder()
                .addRule("(?<year>\\d{4}) ca\\.?")
                .addRule("summer (?<year>\\d{4})")
                .addRule("(?<year>\\d{4})[/\\-](?<month>\\d)[/\\-](?<day>\\d)")
                .addRule("(?<month>\\d{2})/(?<year>\\d{4})")
                .addRule("(?<year>\\d{4})-(?<month>\\d{2})")
                .build();
    }

    private DateRange parseRange(String from, String to, String orig) {
        final LocalDate d1 = parser.parseDateTime(from).toLocalDate();
        final LocalDate d2 = parser.parseDateTime(to).toLocalDate();

        // If we don't have a specific day or month, set these to the appropriate maximum
        // based on the length of the raw date string - a fallible heuristic for sure.
        final String rawDateString = to.replaceAll("\\D", "");
        if (rawDateString.length() < 6) {
            return DateRange.of(
                    d1,
                    d2.with(TemporalAdjusters.lastDayOfYear()),
                    orig
            );
        } else if (rawDateString.length() < 8) {
            return DateRange.of(
                    d1,
                    d2.with(TemporalAdjusters.lastDayOfMonth()),
                    orig
            );
        } else {
            return DateRange.of(d1, d2, orig);
        }
    }

    private DateRange parseSingle(String date, String orig) {
        final LocalDate d = parser.parseDateTime(date).toLocalDate();
        if (date.replaceAll("\\D", "").length() == 4) {
            LocalDate d2 = d.with(TemporalAdjusters.lastDayOfYear());
            return DateRange.of(d, d2, orig);
        }
        return DateRange.of(d, null, orig);
    }

    /**
     * Heuristically attempt to parse a date range. This handles valid dates
     * separated by a hyphen (optionally with surrounding spaces.)
     *
     * @param str a date range string
     * @return a DateRange
     * @throws DateTimeParseException if the string is not parsable
     * @throws DateTimeException if the date is invalid
     */
    public DateRange parse(String str) throws DateTimeException {
        // See if the string matches a year range...
        final Matcher matcher = yearRange.matcher(str);
        if (matcher.matches()) {
            return parseRange(matcher.group("start"), matcher.group("end"), str);
        } else if (str.contains(" - ")) {
            // If it contains a separator with whitespace
            final String[] parts = str.split("\\s-\\s");
            return parseRange(parts[0], parts[1], str);
        } else {
            final int mid = str.length() / 2;
            final char midChar = str.charAt(mid);
            // If the total string is greater or equal to the minimum length
            // for a YEAR-MONTH range and the middle char is a range separator
            // attempt to parse each part as a date...
            if (str.length() > 12 && SEP_CHARS.indexOf(midChar) != -1) {
                // Heuristics: if a string is longer than 12 chars and the
                // middle char is a '-', assume it's a date range...
                return parseRange(
                        str.subSequence(0, mid).toString().trim(),
                        str.subSequence(mid + 1, str.length()).toString().trim(), str);
            } else {
                // Otherwise, attempt to parse as a single date, or fail...
                return parseSingle(str, str);
            }
        }

    }

    /**
     * Heuristically attempt to parse a date range. This handles valid dates
     * separated by a hyphen (optionally with surrounding spaces.)
     *
     * @param str a date range string
     * @return an DateRange if the string is parsable, or an empty optional if not
     */
    public Optional<DateRange> tryParse(String str) {
        try {
            return Optional.of(parse(str));
        } catch (IllegalArgumentException | DateTimeParseException e ) {
            logger.debug(String.format("Unable to parse date range %s", str), e);
            return Optional.empty();
        } catch (DateTimeException e) {
            logger.warn(String.format("Invalid date detected parsing date range %s", str), e);
            return Optional.empty();
        }
    }
}
