/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.importers.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.NoSuchElementException;


public abstract class AbstractImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractImporterTest.class);

    /**
     * Action Manager
     */
    protected ActionManager actionManager;

    /**
     * Test repository, initialised in test setup using TEST_USER identifier.
     */
    protected Repository repository;
    /**
     * Test repository identifier. Depends on fixtures
     */
    protected final String TEST_REPO = "r1";

    /**
     * Convenience method for creating a Sax import manager.
     */
    protected SaxImportManager saxImportManager(Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass, ImportOptions options) {
        return new SaxImportManager(graph, repository, validUser, importerClass, handlerClass, options);
    }

    protected SaxImportManager saxImportManager(Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        return new SaxImportManager(graph, repository, validUser, importerClass, handlerClass, ImportOptions.basic());
    }

    protected SaxImportManager saxImportManager(Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass, String propertiesResource) {
        return saxImportManager(importerClass, handlerClass, ImportOptions.properties(propertiesResource));
    }

    /**
     * Calls setUp in superclass and initialises the repository
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        actionManager = new ActionManager(graph);
        repository = manager.getEntity(TEST_REPO, Repository.class);
    }

    /**
     * Resets the shared import manager after a Test.
     *
     */
    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        logger.debug("Cleaning up after test: reset importManager");
    }

    protected void printGraph(FramedGraph<?> graph) {
        int vcount = 0;
        for (Vertex v : graph.getVertices()) {
            logger.debug(++vcount + " -------------------------");
            for (String key : v.getPropertyKeys()) {
                StringBuilder value = new StringBuilder();
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = v.getProperty(key);
                    for (String o : list) {
                        value.append("[").append(o).append("] ");
                    }
                } else {
                    value = new StringBuilder(v.getProperty(key).toString());
                }
                logger.debug(key + ": " + value);
            }

            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

    // Print a Concept Tree from a 'top' concept down into all narrower concepts
    protected void printConceptTree(final PrintStream out, Concept c) {
        printConceptTree(out, c, 0, "");
    }

    private void printConceptTree(final PrintStream out, Concept c, int depth, String indent) {
        if (depth > 100) {
            out.println("STOP RECURSION, possibly cyclic 'tree'");
            return;
        }

        out.print(indent);
        out.print("[" + c.getIdentifier() + "]");
        Iterable<Description> descriptions = c.getDescriptions();
        for (Description d : descriptions) {
            String lang = d.getLanguageOfDescription();
            String prefLabel = d.getName();
            out.print(", \"" + prefLabel + "\"(" + lang + ")");
        }
        // TODO Print related concept ids?
        for (Concept related : c.getRelatedConcepts()) {
            out.print("[" + related.getIdentifier() + "]");
        }
        for (Concept relatedBy : c.getRelatedByConcepts()) {
            out.print("[" + relatedBy.getIdentifier() + "]");
        }

        out.println("");// end of concept

        indent += ".   ";// the '.' improves readability, but the whole printing could be improved
        for (Concept nc : c.getNarrowerConcepts()) {
            printConceptTree(out, nc, ++depth, indent); // recursive call
        }
    }

    /**
     * Get a Vertex from the FramedGraph using its unit ID.
     *
     * @param graph      the graph to search
     * @param identifier the Vertex's 'human readable' identifier
     * @return the first Vertex with the given identifier
     * @throws NoSuchElementException when there are no vertices with this identifier
     */
    protected Vertex getVertexByIdentifier(FramedGraph<?> graph, String identifier) {
        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, identifier);
        return docs.iterator().next();
    }

    /**
     * Get a Vertex from the FramedGraph using its graph ID.
     *
     * @param graph the graph to search
     * @param id    the Vertex's generated, slugified identifier
     * @return the first Vertex with the given identifier
     * @throws NoSuchElementException when there are no vertices with this identifier
     */
    protected Vertex getVertexById(FramedGraph<?> graph, String id) {
        Iterable<Vertex> docs = graph.getVertices(EntityType.ID_KEY, id);
        return docs.iterator().next();
    }
}
