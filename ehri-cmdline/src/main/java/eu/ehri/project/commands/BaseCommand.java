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

package eu.ehri.project.commands;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Abstract base class for commands. Provides the main
 * entry points for interaction:
 * </p>
 * <ul>
 *     <li>Set options</li>
 *     <li>Get help</li>
 *     <li>Get usage</li>
 *     <li>Execute with options</li>
 * </ul>
 *
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class BaseCommand implements Command {

    private Options options = new Options();
    private CommandLineParser parser = new PosixParser();

    protected void setCustomOptions(Options options) {
    }

    /**
     * Get help for this command.
     *
     * @return a contextual help string
     */
    public abstract String getHelp();

    /**
     * Get usage info.
     *
     * @return a brief usage output
     */
    public abstract String getUsage();

    /**
     * Execute this command with the given database and un-parsed
     * command line arguments
     *
     * @param graph the graph database
     * @param args  the raw command line arguments
     * @return a command return code
     * @throws Exception
     */
    public final int exec(final FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception {
        setCustomOptions(options);
        return execWithOptions(graph, parser.parse(options, args));
    }

    /**
     * Execute this command with the given database and parsed command
     * line arguments.
     *
     * @param graph   the graph database
     * @param cmdLine the parsed command line object
     * @return a command return code
     * @throws Exception
     */
    public abstract int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception;

    /**
     * Utility to get a parsed command line from some args. This is
     * mainly helpful for testing.
     *
     * @param args A list of arg strings
     * @return The parsed command line
     */
    public CommandLine getCmdLine(String[] args) throws ParseException {
        setCustomOptions(options);
        return parser.parse(options, args);
    }

    /**
     * Get an optional log message given a string that may or may not
     * be empty.
     *
     * @param msg a possibly null or empty string
     * @return an optional message string
     */
    protected Optional<String> getLogMessage(String msg) {
        return (msg == null || msg.trim().isEmpty())
                ? Optional.<String>absent()
                : Optional.of(msg);
    }
}
