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
import eu.ehri.project.utils.GraphInitializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Initialise the graph database with minimal nodes.
 */
public class Initialize extends BaseCommand {
    
    final static String NAME = "initialize";


    public Initialize() {
    }

    @Override
    protected void setCustomOptions(Options options) {
    }

    @Override
    public String getHelp() {
        return "Usage: initialize";
    }

    @Override
    public String getUsage() {
        return "Initialize graph DB with minimal nodes (admin account, permissions, types).";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {
        GraphInitializer initializer = new GraphInitializer(graph);
        initializer.initialize();
        return 0;
    }
}
