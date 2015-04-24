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

package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.pipes.util.structures.Pair;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedMap;

/**
 * Utilities for parsing filter and order specifiers
 * for the ad-hoc (and out-dated) query syntax. This
 * is mainly used internally for testing and not exposed
 * externally.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class QueryUtils {

    public static final Logger logger = LoggerFactory.getLogger(QueryUtils.class);

    /**
     * Parse a list of string filter specifications.
     *
     * @param filterList A list of filter spec strings
     * @return A map of filter specs
     */
    static SortedMap<String, Pair<Query.FilterPredicate, String>> parseFilters(
            Iterable<String> filterList) {
        ImmutableSortedMap.Builder<String, Pair<Query.FilterPredicate, String>> builder = new ImmutableSortedMap.Builder<>(
                Ordering.natural());
        Splitter psplit = Splitter.on("__");
        Splitter vsplit = Splitter.on(":");
        for (String filter : filterList) {
            List<String> kv = Iterables.toList(vsplit.split(filter));
            if (kv.size() == 2) {
                String ppred = kv.get(0);
                String value = kv.get(1);
                List<String> pp = Iterables.toList(psplit.split(ppred));
                if (pp.size() == 1) {
                    builder.put(pp.get(0), new Pair<>(
                            Query.FilterPredicate.EQUALS, value));
                } else if (pp.size() > 1) {
                    builder.put(pp.get(0), new Pair<>(
                            Query.FilterPredicate.valueOf(pp.get(1)), value));
                }
            }
        }
        return builder.build();
    }

    /**
     * Parse a list of sort specifications.
     *
     * @param orderSpecs A list of order spec strings
     * @return A map of order specs
     */
    static SortedMap<String, Query.Sort> parseOrderSpecs(Iterable<String> orderSpecs) {
        ImmutableSortedMap.Builder<String, Query.Sort> builder = new ImmutableSortedMap.Builder<>(
                Ordering.natural());
        Splitter psplit = Splitter.on("__");
        for (String spec : orderSpecs) {
            List<String> od = Iterables.toList(psplit.split(spec));
            if (od.size() == 1) {
                builder.put(od.get(0), Query.Sort.ASC);
            } else if (od.size() > 1) {
                builder.put(od.get(0), Query.Sort.valueOf(od.get(1)));
            }
        }
        return builder.build();
    }

    /**
     * Class that represents a traversal path to a particular
     * property of some relations.
     */
    public static class TraversalPath {
        private final String property;
        private final List<Pair<String,Direction>> traversals;

        public TraversalPath(String property, List<Pair<String,Direction>> traversals) {
            this.property = property;
            this.traversals = traversals;
        }

        public String getProperty() {
            return property;
        }

        public List<Pair<String, Direction>> getTraversals() {
            return traversals;
        }

        @Override public String toString() {
            // TODO: Fix this...
            return traversals + " : " + property;
        }
    }

    /**
     * Attempt to parse a traversal pattern from a input string. The
     * string should look like this:
     *
     *  ->relation1->relation2.propertyName
     *
     * @param input A traversal path spec
     * @return A traversal path
     */
    public static Optional<TraversalPath> getTraversalPath(String input) {

        String[] splitProp = input.split("\\.");
        if (splitProp.length != 2) {
            logger.warn("Ignoring invalid traversal path without property delimiter: {}", input);
            return Optional.absent();
        }
        String pathSpec = splitProp[0];
        String property = splitProp[1];

        // Arrrgh, lookarounds and splitting on zero-width matches, inspired
        // by http://stackoverflow.com/questions/275768/is-there-a-way-to-split-strings-with-string-split-and-include-the-delimiters
        String[] parse = pathSpec.split("(?<=\\w+)(?=->)|(?<=->)(?=\\w+)|(?<=\\w+)(?=<-)|(?<=<-)");

        // if it has an odd number of values it must end in
        // a direction delimiter, so it's invalid.
        // Only if there's an odd number of elements are we okay.
        if (parse.length % 2 != 0) {
            logger.warn("Ignoring invalid traversal path pattern: {}", input);
            return Optional.absent();
        }

        List<Pair<String,Direction>> traversals = Lists.newArrayListWithCapacity(parse.length / 2);
        for (int i = 0; i < parse.length; i += 2) {
            String dir = parse[i];
            String rel = parse[i+1];
            if (rel.equals("->") || rel.equals("<-") ) {
                logger.warn("Invalid traversal path starts with delimiter: {}", input);
                return Optional.absent();
            }
            if (dir.equals("->")) {
                traversals.add(new Pair<>(rel, Direction.OUT));
            } else {
                traversals.add(new Pair<>(rel, Direction.IN));
            }
        }
        return Optional.of(new TraversalPath(property, traversals));
    }
}
