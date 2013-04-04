/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.test.AbstractFixtureTest;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.junit.Test;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;


/**
 * @author linda
 */
public class AbstractImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractImporterTest.class);
    
    @Test
    public void testEhriImporterUser(){
        int count = getNodeCount(graph);
        logger.debug("count of nodes before importing: " + count);
        final String SINGLE_EAC = "algemeyner-yidisher-arbeter-bund-in-lite-polyn-un-rusland.xml";

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAC);
        try {
            ImportLog log = new SaxImportManager(graph, SystemScope.getInstance(), validUser, EacImporter.class,
                            EacHandler.class).setTolerant(Boolean.TRUE).importFile(ios, "testing the AbstractImporter");
            assertTrue(65 < getNodeCount(graph));
            Vertex v = getVertexByIdentifier(graph, "ehriimporter");
            assertNotNull(v);
            for(String key : v.getPropertyKeys())
                logger.warn(key + ": " + v.getProperty(key));
            UserProfile up = graph.frame(v, UserProfile.class);
            assertTrue(0 < toList(up.getGroups()).size());
            for(Group g : up.getGroups()){
                logger.warn(g.getIdentifier() + ": " + g.getType());
                logger.warn(toList(g.getPermissionGrants()).size() +"");
            }
//            printGraph(graph);
        } catch (Exception ex) {
            logger.error("error: " + ex.getMessage());
            }
    }
    protected void printGraph(FramedGraph<Neo4jGraph> graph) {
        for (Vertex v : graph.getVertices()) {
            logger.debug("-------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = (String[]) v.getProperty(key);
                    for (String o : list) {
                        value += "["+o.toString() + "] ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                logger.debug(key + ": " + value);
            }
//            
            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

    protected Vertex getVertexByIdentifier(FramedGraph<Neo4jGraph> graph, String id) {
        Iterable<Vertex> docs = graph.getVertices(IdentifiableEntity.IDENTIFIER_KEY, id);
        return docs.iterator().next();
    }

    protected int getNodeCount(FramedGraph<Neo4jGraph> graph) {
        return toList(GlobalGraphOperations
                .at(graph.getBaseGraph().getRawGraph()).getAllNodes()).size();
    }
}
