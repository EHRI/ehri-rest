package eu.ehri.project.commands;

import com.google.common.base.Optional;
import eu.ehri.project.exceptions.SerializationError;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Serializer;

import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * Import EAD from the command line...
 */
public class ListEntities extends BaseCommand implements Command {

    final static String NAME = "list";

    /**
     * Constructor.
     */
    public ListEntities() {
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    protected void setCustomOptions() {
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
        String help = "List entities of a given type.";
        return help;
    }

    /**
     * Command-line entry-point (for testing.)
     *
     * @throws Exception
     */
    @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {

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
            String format = (String) cmdLine.getOptionValue("f");
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

    /**
     * Output node IDs only.
     *
     * @param manager
     * @param type
     */
    private void printIds(GraphManager manager, EntityClass type) {
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            System.out.println(manager.getId(acc));
        }
    }

    /**
     * Output nodes as JSON.
     *
     * @param manager
     * @param serializer
     * @param type
     * @throws SerializationError
     */
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

    /**
     * Output nodes as XML, with a given transformer.
     *
     * @param manager
     * @param serializer
     * @param type
     * @param transformer
     * @param rootName
     * @throws SerializationError
     * @throws TransformerException
     * @throws UnsupportedEncodingException
     */
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

    /**
     * Get an XML Document transformer...
     *
     * @param xsltOpt Optional XSLT InputStream
     * @throws java.io.IOException
     * @throws javax.xml.transform.TransformerException
     *
     */
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