package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Serializer;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Import EAD from the command line...
 * 
 */
public class ListEntities extends BaseCommand implements Command {
    
    final static String NAME = "list";

    /**
     * Constructor.
     */
    public ListEntities() {
    }

    @Override
    protected void setCustomOptions() {
        options.addOption(OptionBuilder
                .withType(String.class)
                .withLongOpt("format").isRequired(false)
                .hasArg(true).withArgName("f")
                .withDescription("Format for output data, which defaults to just the id. " +
                        "If provided can be one of: xml, json").create("f"));
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

        if (!cmdLine.hasOption("f")) {
        	// default to only outputting the id's
        	printIds(graph, cmdLine);    
        } else {
            // if there is a second argument, that might be 'json' or 'xml'
        	String format = (String)cmdLine.getOptionValue("f");
        	if (format.equalsIgnoreCase("xml")) {
        		printXml(graph, cmdLine);
        	} else if (format.equalsIgnoreCase("json")) {
        		printJson(graph, cmdLine);
        	} else {
        		// unknown format
        		throw new RuntimeException("Unknown format: " + format);
        	}
        }
        
        return 0;
    }
    
    private void printIds(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);        
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            System.out.println(manager.getId(acc));
        }
    }
    
    private void printJson(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);

        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

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

    private void printXml(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        Serializer serializer = new Serializer(graph);

        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getEntityClass();

        if (!AccessibleEntity.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        System.out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        System.out.print("<list>\n"); // root element
        
        for (AccessibleEntity acc : manager.getFrames(type, AccessibleEntity.class)) {
            printDocument(serializer.vertexFrameToXml(acc), System.out);
        }
        
        System.out.print("</list>\n"); // root element
    }

    /**
     * Pretty-print an XML document.
     *
     * @param doc
     * @param out
     * @throws java.io.IOException
     * @throws javax.xml.transform.TransformerException
     */
    private static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
