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

import com.tinkerpop.blueprints.impls.sail.SailGraph;
import com.tinkerpop.blueprints.oupls.sail.pg.PropertyGraphSail;
import com.tinkerpop.frames.FramedGraph;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Export to RDF.
 *
 * NB: This current has a problem with TP 2.4.0 which
 * causes a crash on array properties. It seems to have
 * been fixed on TP 2.5.0-SNAPSHOT.
 */
public class RdfExport extends BaseCommand {

    public final static String NAME = "export-rdf";

    public final static String DEFAULT_FORMAT = "turtle";

    @Override
    public String getHelp() {
        return "export low-level graph structure as RDF";
    }

    @Override
    public String getUsage() {
        String sep = System.getProperty("line.separator");
        StringBuilder buffer = new StringBuilder(String.format(
                "Usage: %s -f [format] <filename>%n%n " +
                        "Accepted formats are: %n%n", NAME));
        for (String fmt : SailGraph.formats.keySet()) {
            buffer.append("  ");
            buffer.append(fmt);
            buffer.append(sep);
        }
        buffer.append(sep);
        buffer.append("The default format is: " + DEFAULT_FORMAT);
        return buffer.toString();
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(new Option("format", "f", true, "RDF format"));
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1) {
            throw new MissingArgumentException("Output file path missing");
        }
        String fmt = cmdLine.getOptionValue("format", DEFAULT_FORMAT);

        PropertyGraphSail propertyGraphSail = new PropertyGraphSail(graph.getBaseGraph(), false);
        SailGraph sailGraph = new SailGraph(propertyGraphSail);
        try (OutputStream outputStream = new FileOutputStream((String) cmdLine.getArgList().get(0))) {
            sailGraph.saveRDF(outputStream, fmt);
        }

        return 0;
    }
}
