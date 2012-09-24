package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.map.HashedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public class EadImporter extends BaseImporter<Node> {

    private final XPath xpath;

    public EadImporter(FramedGraph<Neo4jGraph> framedGraph, Object repositoryId) {
        super(framedGraph, repositoryId);
        xpath = XPathFactory.newInstance().newXPath();

    }

    @Override
    public void importDocumentaryUnit(Node data) throws Exception {
        // TODO Auto-generated method stub
        XPathExpression expr = xpath
                .compile(".//archdesc/did/unittitle/text()");
        System.out.println(expr.evaluate(data, XPathConstants.STRING));
    }

    @Override
    public EntityBundle<DocumentaryUnit> extractDocumentaryUnit(Node data)
            throws ValidationError {
        EntityBundle<DocumentaryUnit> unit = new BundleFactory<DocumentaryUnit>()
                .buildBundle(new HashMap<String, Object>(),
                        DocumentaryUnit.class);
        
        
        return unit;
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2)
            throw new RuntimeException(
                    "Usage: importer <ead.xml> <neo4j-graph-dir>");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(args[0]);

        Neo4jGraph graph = new Neo4jGraph(args[1]);
        EadImporter importer = new EadImporter(new FramedGraph<Neo4jGraph>(
                graph), 1);

        XPathFactory xfactory = XPathFactory.newInstance();
        XPath xpath = xfactory.newXPath();
        XPathExpression expr = xpath.compile("//eadlist/ead");

        NodeList result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        
        
        try {
            graph.getRawGraph().beginTx();
            for (int i = 0; i < result.getLength(); i++) {
                importer.importDocumentaryUnit(result.item(i));
            }
            graph.stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            graph.stopTransaction(Conclusion.FAILURE);            
        } finally {
            graph.shutdown();
        }
    }
}
