package eu.ehri.project.views.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.pipes.util.structures.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: michaelb
 * Date: 30/01/13
 * Time: 15:09
 * To change this template use File | Settings | File Templates.
 */
public class QueryUtils {

    public static Logger logger = LoggerFactory.getLogger(QueryUtils.class);

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
     * @param input
     * @return
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

        List<Pair<String,Direction>> traversals = Lists.newLinkedList();
        for (int i = 0; i < parse.length; i += 2) {
            String dir = parse[i];
            String rel = parse[i+1];
            if (rel.equals("->") || rel.equals("<-") ) {
                logger.warn("Invalid traversal path starts with delimiter: {}", input);
                return Optional.absent();
            }
            if (dir.equals("->")) {
                traversals.add(new Pair<String,Direction>(rel, Direction.OUT));
            } else {
                traversals.add(new Pair<String,Direction>(rel, Direction.IN));
            }
        }
        return Optional.of(new TraversalPath(property, traversals));
    }
}
