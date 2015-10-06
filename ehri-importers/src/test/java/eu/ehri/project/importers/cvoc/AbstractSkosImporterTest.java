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

package eu.ehri.project.importers.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AbstractSkosImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSkosImporterTest.class);
    public static final String FILE1 = "cvoc/simple.xml";
    public static final String FILE2 = "cvoc/simple.n3";
    public static final String FILE3 = "cvoc/repository-types.xml";
    public static final String FILE4 = "cvoc/camps.rdf";
    public static final String FILE5 = "cvoc/ghettos.rdf";

    protected Actioner actioner;
    protected ActionManager actionManager;
    protected Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actioner = manager.cast(validUser, Actioner.class);
        vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
        actionManager = new ActionManager(graph);
    }
    
     protected void printGraph(FramedGraph<?> graph) {
        int vcount = 0;
        for (Vertex v : graph.getVertices()) {
            logger.debug(++vcount + " -------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = (String[]) v.getProperty(key);
                    for (String o : list) {
                        value += "[" + o + "] ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                logger.debug(key + ": " + value);
            }

            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

}
