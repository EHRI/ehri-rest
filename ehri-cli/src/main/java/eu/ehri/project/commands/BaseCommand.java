/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ApiFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Abstract base class for commands. Provides the main
 * entry points for interaction:
 * <p>
 * <ul>
 * <li>Set options</li>
 * <li>Get help</li>
 * <li>Get usage</li>
 * <li>Execute with options</li>
 * </ul>
 */
public abstract class BaseCommand implements Command {

    static final Logger logger = org.slf4j.LoggerFactory.getLogger(Command.class);
    protected final Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();

    protected void setCustomOptions(Options options) {
    }

    public abstract String getHelp();

    public abstract String getUsage();

    /**
     * Execute this command with the given database and un-parsed
     * command line arguments
     *
     * @param graph the graph database
     * @param args  the raw command line arguments
     * @return a command return code
     */
    public final int exec(FramedGraph<?> graph, String[] args) throws Exception {
        setCustomOptions(options);
        return execWithOptions(graph, parser.parse(options, args));
    }

    @Override
    public String getDetailedHelp() {
        HelpFormatter formatter = new HelpFormatter();
        setCustomOptions(options);
        StringWriter out = new StringWriter();
        formatter.printHelp(
                new PrintWriter(out), 80, getUsage(), "\n" + getHelp() + "\n\n",
                options, 5, 0, "\n" + getHelpFooter());
        return out.toString();
    }


    /**
     * Execute this command with the given database and parsed command
     * line arguments.
     *
     * @param graph   the graph database
     * @param cmdLine the parsed command line object
     * @return a command return code
     */
    public abstract int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception;

    /**
     * Utility to get a parsed command line from some args. This is
     * mainly helpful for testing.
     *
     * @param args A list of arg strings
     * @return The parsed command line
     */
    CommandLine getCmdLine(String[] args) throws ParseException {
        setCustomOptions(options);
        return parser.parse(options, args);
    }

    protected Api api(FramedGraph<?> graph, Accessor accessor) {
        return ApiFactory.withLogging(graph, accessor);
    }

    /**
     * Get an optional log message given a string that may or may not
     * be empty.
     *
     * @param msg a possibly null or empty string
     * @return an optional message string
     */
    Optional<String> getLogMessage(String msg) {
        return (msg == null || msg.trim().isEmpty()) ? Optional.empty() : Optional.of(msg);
    }
}
