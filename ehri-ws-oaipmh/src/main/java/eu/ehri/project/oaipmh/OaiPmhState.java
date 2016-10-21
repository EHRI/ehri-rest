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

package eu.ehri.project.oaipmh;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.ehri.project.oaipmh.errors.OaiPmhArgumentError;
import eu.ehri.project.oaipmh.errors.OaiPmhError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles validation of OAI-PMH state parameters and
 * resumption token serialization.
 */
public class OaiPmhState {

    private static final Logger log = LoggerFactory.getLogger(OaiPmhState.class);
    private static final Splitter queryStringSplitter = Splitter.on('&');
    private static final Splitter queryStringArgSplitter = Splitter.on('=');
    private static final Joiner queryStringJoiner = Joiner.on('&');
    private static final Joiner queryStringArgJoiner = Joiner.on('=');

    private static final String OFFSET_PARAM = "offset";
    private static final String LIMIT_PARAM = "limit";

    private static final String VERB_PARAM = "verb";
    private static final String METADATA_PREFIX_PARAM = "metadataPrefix";
    private static final String IDENTIFIER_PARAM = "identifier";
    private static final String SET_PARAM = "set";
    private static final String FROM_PARAM = "from";
    private static final String UNTIL_PARAM = "until";
    private static final String RESUMPTION_TOKEN_PARAM = "resumptionToken";

    private static final Pattern SHORT_TIME_FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern LONG_TIME_FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");


    private final int offset;
    private final int limit;
    private final Verb verb;
    private final String identifier;
    private final MetadataPrefix prefix;
    private final String setSpec;
    private final String from;
    private final String until;

    private OaiPmhState(int offset, int limit, Verb verb, String identifier,
            MetadataPrefix prefix, String setSpec, String from, String until) throws OaiPmhError {
        this.offset = offset;
        this.limit = limit;
        this.verb = verb;
        this.identifier = identifier;
        this.prefix = prefix;
        this.setSpec = setSpec;
        this.from = from;
        this.until = until;

        validateState();
    }

    private OaiPmhState(Verb verb, String identifier,
            MetadataPrefix prefix, String setSpec, String from, String until, int defaultLimit)
            throws OaiPmhError {
        this(0, defaultLimit, verb, identifier, prefix, setSpec, from, until);
    }

    private void validateState() throws OaiPmhArgumentError {
        // Logical consistency checks
        if (verb != null && verb.equals(Verb.GetRecord) && identifier == null) {
            throw new OaiPmhArgumentError("No identifier given for " + verb);
        }
        if (verb != null && (verb.equals(Verb.ListIdentifiers) || verb.equals(Verb.ListRecords)
                || verb.equals(Verb.GetRecord))
                && prefix == null) {
            throw new OaiPmhArgumentError("No metadataPrefix given for " + verb);
        }
        if (from != null && until != null && from.compareTo(until) > 0) {
            throw new OaiPmhArgumentError("Date 'from' filter must be before 'until'");
        }
        if (from != null && until != null && from.length() != until.length()) {
            throw new OaiPmhArgumentError("Date filters 'from' and 'until' must have the same granularity");
        }
        validateTime(FROM_PARAM, from);
        validateTime(UNTIL_PARAM, until);
    }

    private static class Builder {
        int offset;
        int limit;
        Verb verb = Verb.Identify;
        String identifier = null;
        MetadataPrefix prefix;
        String setSpec = null;
        String from = null;
        String until = null;
        final int defaultLimit;

        Builder(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        OaiPmhState build() throws OaiPmhError {
            return offset > 0
                    ? new OaiPmhState(offset, limit, verb, identifier, prefix, setSpec, from, until)
                    : new OaiPmhState(verb, identifier, prefix, setSpec, from, until, defaultLimit);
        }
    }

    int getOffset() {
        return offset;
    }

    int getLimit() {
        return limit;
    }

    boolean hasLimit() {
        return limit >= 0;
    }

    Verb getVerb() {
        return verb;
    }

    String getIdentifier() {
        return identifier;
    }

    MetadataPrefix getMetadataPrefix() {
        return prefix;
    }

    String getSetSpec() {
        return setSpec;
    }

    String getFrom() {
        return from;
    }

    String getUntil() {
        return until;
    }

    boolean shouldResume(int count) {
        return limit > 0 && count > offset + limit;
    }

    boolean hasResumed() {
        return offset > 0;
    }

    String nextState() {
        return encodeToken(next());
    }

    Map<String, String> toMap() {
        return mapOf(
                VERB_PARAM, verb != null ? verb.name() : null,
                IDENTIFIER_PARAM, identifier,
                METADATA_PREFIX_PARAM, prefix != null ? prefix.name() : null,
                SET_PARAM, setSpec,
                FROM_PARAM, from,
                UNTIL_PARAM, until
        );
    }

    private OaiPmhState next() {
        try {
            return new OaiPmhState(offset + limit, limit,
                    verb, identifier, prefix, setSpec, from, until);
        } catch (OaiPmhError error) {
            // This will never happen since we've validated the
            // params in the constructor.
            throw new RuntimeException();
        }
    }

    private ImmutableMultimap<String, String> toParams() {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.<String, String>builder()
                .put(OFFSET_PARAM, String.valueOf(offset))
                .put(LIMIT_PARAM, String.valueOf(limit))
                .put(VERB_PARAM, verb.name());
        if (prefix != null) {
            builder.put(METADATA_PREFIX_PARAM, prefix.name());
        }
        if (from != null) {
            builder.put(FROM_PARAM, from);
        }
        if (until != null) {
            builder.put(UNTIL_PARAM, until);
        }
        if (identifier != null) {
            builder.put(IDENTIFIER_PARAM, identifier);
        }
        if (setSpec != null) {
            builder.put(SET_PARAM, setSpec);
        }
        return builder.build();
    }

    public static OaiPmhState parse(String rawParams, int defaultLimit) throws OaiPmhError {
        ImmutableMultimap<String, String> queryParams = decodeParams(rawParams);
        checkDuplicateArguments(queryParams);
        String token = queryParams.containsKey(RESUMPTION_TOKEN_PARAM)
                ? queryParams.get(RESUMPTION_TOKEN_PARAM).iterator().next()
                : null;
        if (token != null && !token.trim().isEmpty()) {
            // Check token is the exclusive argument
            if (queryParams.size() > 2) {
                throw new OaiPmhError(ErrorCode.badResumptionToken,
                        "Resumption token must be an exclusive argument in addition to the verb");
            }

            String tokenQueryString = decodeBase64(token);
            log.trace("Decoding state: {}", tokenQueryString);
            return buildState(decodeParams(tokenQueryString), defaultLimit);
        } else {
            return buildState(queryParams, defaultLimit);
        }
    }

    private static OaiPmhState buildState(ImmutableMultimap<String, String> queryParams, int defaultLimit) throws OaiPmhError {
        Builder builder = new Builder(defaultLimit);
        for (Map.Entry<String, String> entry : queryParams.entries()) {
            switch (entry.getKey()) {
                case VERB_PARAM:
                    builder.verb = Verb.parse(entry.getValue(), Verb.Identify);
                    break;
                case IDENTIFIER_PARAM:
                    builder.identifier = entry.getValue();
                    break;
                case METADATA_PREFIX_PARAM:
                    builder.prefix = MetadataPrefix.parse(entry.getValue(), MetadataPrefix.oai_dc);
                    break;
                case SET_PARAM:
                    builder.setSpec = entry.getValue();
                    break;
                case FROM_PARAM:
                    builder.from = entry.getValue();
                    break;
                case UNTIL_PARAM:
                    builder.until = entry.getValue();
                    break;
                case OFFSET_PARAM:
                    builder.offset = Integer.parseInt(entry.getValue());
                    break;
                case LIMIT_PARAM:
                    builder.limit = Integer.parseInt(entry.getValue());
                    break;
                default:
                    throw new OaiPmhArgumentError("Unexpected argument: " + entry.getKey());
            }
        }
        return builder.build();
    }

    // Helpers

    private static void validateTime(String key, String time) throws OaiPmhArgumentError {
        if (time != null) {
            Matcher p1 = SHORT_TIME_FORMAT.matcher(time);
            Matcher p2 = LONG_TIME_FORMAT.matcher(time);
            if (!p1.matches() && !p2.matches()) {
                throw new OaiPmhArgumentError("Invalid time given for " + key + ": " + time);
            }
        }
    }

    private static void checkDuplicateArguments(ImmutableMultimap<String, String> queryParams)
            throws OaiPmhArgumentError {
        for (Map.Entry<String, Collection<String>> e : queryParams.asMap().entrySet()) {
            if (e.getValue().size() > 1) {
                throw new OaiPmhArgumentError("Duplicate value for parameter " + e.getKey());
            }
        }
    }

    private static String encodeToken(OaiPmhState state) {
        String stateParams = encodeParams(state.toParams());
        log.trace("Encoding state: {}", stateParams);
        return encodeBase64(stateParams);
    }

    private static String encodeParams(Multimap<String, String> params) {
        List<String> parts = params.entries().stream()
                .map(e -> queryStringArgJoiner.join(encodeUrlParam(e.getKey()), encodeUrlParam(e.getValue())))
                .collect(Collectors.toList());
        return queryStringJoiner.join(parts);
    }

    private static ImmutableMultimap<String, String> decodeParams(String params) {
        ImmutableMultimap.Builder<String, String> b = ImmutableMultimap.builder();
        if (params != null) {
            queryStringSplitter.splitToList(params).stream()
                    .map(p -> queryStringArgSplitter.limit(2).splitToList(p))
                    .filter(p -> p.size() == 2)
                    .forEach(p -> b.put(decodeUrlParam(p.get(0)), decodeUrlParam(p.get(1))));
        }
        return b.build();
    }

    private static String decodeUrlParam(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeUrlParam(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String decodeBase64(String s) throws OaiPmhError {
        try {
            return new String(Base64.getDecoder().decode(s));
        } catch (IllegalArgumentException e) {
            throw new OaiPmhError(ErrorCode.badResumptionToken, "Invalid resumption token: " + s);
        }
    }

    private static String encodeBase64(String s) {
        return new String(Base64.getEncoder().encode(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, String> mapOf(String... items) {
        Preconditions.checkArgument(items.length % 2 == 0, "Items must be pairs of key/value");
        Map<String, String> map = Maps.newHashMap();
        for (int i = 0; i < items.length; i += 2) {
            map.put((items[i]), items[i + 1]);
        }
        return map;
    }
}
