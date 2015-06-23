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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistence.Serializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Command for listing entities of a given type.
 *
 * @author Mike Bryant (https://github.com/mikesname)
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class ListEntities extends BaseCommand {

    final static String NAME = "list";

    @Override
    @SuppressWarnings("static-access")
    protected void setCustomOptions(Options options) {
        options.addOption(OptionBuilder
           .withType(String.class)
           .withLongOpt("format").isRequired(false)
           .hasArg(true).withArgName("f")
           .withDescription("Format for output data, which defaults to just the id. " +
                   "If provided can be one of: xml, json").create("f"));
        options.addOption(OptionBuilder
                .withType(String.class)
                .withLongOpt("root-node").isRequired(false)
                .hasArg(true).withArgName("r")
                .withDescription("Name of the root node (default: '" + NAME + "')").create("r"));
    }

    @Override
    public String getHelp() {
        return "Usage: list [OPTIONS] <type>";
    }

    @Override
    public String getUsage() {
        return "List entities of a given type.";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {

        // the first argument is the entity type, and that must be specified
        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);
        String rootName = cmdLine.getOptionValue("r", NAME);

        if (!cmdLine.hasOption("f")) {
            // default to only outputting the id's
            printIds(manager, type);
        } else {
            // if there is a second argument, that might be 'json' or 'xml'
            String format = cmdLine.getOptionValue("f");
            if (format.equalsIgnoreCase("xml")) {
                printXml(manager, serializer, type, getTransformer(Optional.<InputStream>absent()),
                        rootName);
            } else if (format.equalsIgnoreCase("json")) {
                printJson(manager, serializer, type);
            } else {
                // If there's an XSLT file in the resources that is named
                // EntityType_destinationFormat.xslt use that...
                String xsltName = String.format("%s_%s.xslt", type.getName(), format);
                InputStream ios = EntityClass.class.getClassLoader().getResourceAsStream(xsltName);
                if (ios != null) {
                    try {
                        Transformer transformer = getTransformer(Optional.fromNullable(ios));
                        printXml(manager, serializer, type, transformer, rootName);
                    } finally {
                        ios.close();
                    }
                } else {
                    // unknown format
                    throw new RuntimeException(
                            String.format("Unknown format: '%s' and no '%s' xslt found", format, xsltName));
                }
            }
        }

        return 0;
    }

    private void printIds(GraphManager manager, EntityClass type) {
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            System.out.println(acc.getId());
        }
    }

    private void printJson(GraphManager manager, Serializer serializer, EntityClass type)
            throws SerializationError {

        // NOTE no json root {}, but always a list []
        System.out.print("[\n");

        int cnt = 0;
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            String jsonString = serializer.vertexFrameToJson(acc);
            if (cnt != 0) System.out.print(",\n");
            System.out.println(jsonString);
            cnt++;
        }
        System.out.print("]\n"); // end list

    }

    private void printXml(GraphManager manager, Serializer serializer, EntityClass type,
            Transformer transformer, String rootName)
                throws UnsupportedEncodingException, SerializationError, TransformerException {
        System.out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        System.out.print("<" + rootName + ">\n"); // root element
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            transformer.transform(new DOMSource(serializer.vertexFrameToXml(acc)),
                    new StreamResult(new OutputStreamWriter(System.out, "UTF-8")));
        }
        System.out.print("</" + rootName + ">\n"); // root element
    }

    private static Transformer getTransformer(Optional<InputStream> xsltOpt) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = xsltOpt.isPresent()
                ? tf.newTransformer(new StreamSource(xsltOpt.get()))
                : tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return transformer;
    }
}