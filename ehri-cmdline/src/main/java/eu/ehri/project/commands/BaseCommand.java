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

public abstract class BaseCommand implements Command {
    
    Options options = new Options();
    CommandLineParser parser = new PosixParser();
        
    protected void setCustomOptions() {}

    public abstract String getHelp();
    public abstract String getUsage();

    public final int exec(final FramedGraph<? extends TransactionalGraph> graph, String[] args) throws Exception {
        setCustomOptions();
        return execWithOptions(graph, parser.parse(options, args));
    }
    public abstract int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws Exception;

    /**
     * Utility to get a parsed command line from some args. This is
     * mainly helpful for testing.
     * @param args A list of arg strings
     * @return The parsed command line
     */
    public CommandLine getCmdLine(String[] args) throws ParseException {
        setCustomOptions();
        return parser.parse(options, args);
    }

    protected Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
