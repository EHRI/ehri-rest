package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Serializer;

//import com.fasterxml.jackson.xml.XmlMapper;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

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

        if (cmdLine.getArgList().size() < 2) {
        	// default to only outputting the id's
        	printIds(graph, cmdLine);    
        } else {
            // if there is a second argument, that might be 'json' or 'xml'
        	String format = cmdLine.getArgs()[1];
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
        	        	
        	// First to json then to xml, but we could go from bundle to xml
        	String jsonString = serializer.vertexFrameToJson(acc);
        	// NOTE: we could implement the XML production in a DataConverter.bundleToXml
        	

        	// NOTE: I was hoping that the 'Jackson' json lib could also generate XML !
        	// Then I could use JSONObject jsonObject = new JSONObject(jsonString);
        	// 
        	// However now I need another lib to do it: 
        	// produce XML, using XMlSerializer from json-lib 
        	net.sf.json.xml.XMLSerializer xmlSerializer = new net.sf.json.xml.XMLSerializer();
        	net.sf.json.JSON json = net.sf.json.JSONSerializer.toJSON( jsonString ); 
            String xmlString = xmlSerializer.write( json );
            xmlString = xmlString.substring(xmlString.indexOf('\n')+1); // remove instruction
            System.out.print(xmlString);   
        }
        
        System.out.print("</list>\n"); // root element
    }
}
