package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public class EadImporter extends BaseImporter<Node> {

	private final XPath xpath;

	public EadImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository) {
		super(framedGraph, repository);
		xpath = XPathFactory.newInstance().newXPath();

	}

	// Pattern for EAD nodes that represent a child item
	Pattern childItemPattern = Pattern.compile("^c(?:\\d+)$");

	@Override
	public void importDocumentaryUnit(Node data) throws Exception {
		// TODO Auto-generated method stub
		XPathExpression expr = xpath
				.compile(".//archdesc/did/unittitle/text()");
		System.out.println(expr.evaluate(data, XPathConstants.STRING));
	}

	public List<Node> extractChildData(Node data) {
		List<Node> children = new LinkedList<Node>();
		for (int i = 0; i < data.getChildNodes().getLength(); i++) {
			Node n = data.getChildNodes().item(i);
			if (childItemPattern.matcher(n.getNodeName()).matches()) {
				children.add(n);
			}
		}
		return children;
	}

	@Override
	public EntityBundle<DocumentaryUnit> extractDocumentaryUnit(Node data)
			throws Exception {
		EntityBundle<DocumentaryUnit> unit = new BundleFactory<DocumentaryUnit>()
				.buildBundle(new HashMap<String, Object>(),
						DocumentaryUnit.class);
		
		unit = unit.setDataValue("identifier", xpath
				.compile(".//archdesc/did/unitid/text()").evaluate(data, XPathConstants.STRING));
		unit = unit.setDataValue("name", xpath
				.compile(".//archdesc/did/unittitle/text()").evaluate(data, XPathConstants.STRING));
		System.out.println("Extract item: " + unit.getData().get("identifier"));
		return unit;
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption(new Option("createrepo", false, "Create agent with the given ID"));
		CommandLineParser parser = new PosixParser();
		CommandLine cmdLine = parser.parse(options, args);
		
		if (cmdLine.getArgList().size() != 3)
			throw new RuntimeException(
					"Usage: importer [OPTIONS] <agent-id> <ead.xml> <neo4j-graph-dir>");

		// Get the graph and search it for the required agent...
		FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
				new Neo4jGraph((String) cmdLine.getArgList().get(2)));
		
		Agent agent = null;
		if (cmdLine.hasOption("createrepo")) {
			try {
				Map<String, Object> agentData = new HashMap<String, Object>();
				agentData.put("identifier", cmdLine.getArgList().get(0));
				agentData.put("name", cmdLine.getArgList().get(0));
				EntityBundle<Agent> agb = new BundleFactory<Agent>().buildBundle(agentData, Agent.class);
				agent = new BundleDAO<Agent>(graph).insert(agb);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			agent = graph.getVertices("identifier", (String)cmdLine.getArgList().get(0), Agent.class)
					.iterator().next();
		}

		// XML parsing boilerplate...
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse((String)cmdLine.getArgList().get(1));

		// Extract the EAD instances from the eadlist.
		XPathFactory xfactory = XPathFactory.newInstance();
		XPath xpath = xfactory.newXPath();
		XPathExpression expr = xpath.compile("//eadlist/ead");

		NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

		
		try {
			// Initialize the importer...
			EadImporter importer = new EadImporter(graph, agent);

			for (int i = 0; i < result.getLength(); i++) {				
				importer.importDetails(result.item(i));
			}
			graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
		} finally {
			graph.shutdown();
		}
	}
}
