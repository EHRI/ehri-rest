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
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.FileInputStream;

/**
 * Command for loading a fixtures file into the graph.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class LoadFixtures extends BaseCommand {

    final static String NAME = "load-fixtures";

    public LoadFixtures() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("init",
                "Initialize graph before loading fixtures"));
    }

    @Override
    public String getHelp() {
        return "Usage: load-fixtures";
    }

    @Override
    public String getUsage() {
        return "Load the fixtures into the database.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {
        boolean initialize = cmdLine.hasOption("init");
        FixtureLoader loader = FixtureLoaderFactory.getInstance(graph, initialize);
        if (cmdLine.getArgList().size() == 1) {
            String path = cmdLine.getArgs()[0];
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                throw new RuntimeException(String.format(
                        "Fixture file: '%s does not exist or is not a file", path));
            }
            System.err.println("Loading fixture file: " + path);
            try (FileInputStream inputStream = new FileInputStream(file)) {
                loader.loadTestData(inputStream);
            }
        } else {
            // Load default fixtures...
            loader.loadTestData();
        }

        return 0;
    }
}
