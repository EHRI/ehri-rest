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

import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.CommandLine;

/**
 * Command-line tools to perform actions on the graph database need
 * to specify a help text about the functionality of the tool and
 * a string to explain their usage.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public interface Command {
    /**
     * Get information about the functionality of the command.
     *
     * @return a help text
     */
    String getHelp();

    /**
     * Get information about the usage of the command, including
     * required and/or optional parameters.
     *
     * @return a usage text
     */
    String getUsage();

    /**
     * Execute this command with the given command line options.
     *
     * @param graph   the graph database
     * @param cmdLine the command line options
     * @return a status code (0 = success)
     * @throws Exception
     */
    int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception;

    /**
     * Execute this command with the given arguments.
     *
     * @param graph the graph database
     * @param args  the raw argument strings
     * @return a status code (0 = success)
     * @throws Exception
     */
    int exec(FramedGraph<?> graph, String[] args) throws Exception;
}
