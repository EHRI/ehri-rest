package eu.ehri.project.importers.cvoc;

import java.io.InputStream;
import java.io.PrintStream;

import com.google.common.collect.Iterables;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.base.AccessibleEntity;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author paulboon
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SkosCoreCvocImporterTest extends AbstractImporterTest { //AbstractFixtureTest {
    // Note we only can do RSF
    protected final String SKOS_FILE = "ehri-skos.rdf";//"broaderskos.xml";//"skos.rdf";

    @Test
    public void testImportItemsT() throws Exception {
    	final String logMessage = "Importing a SKOS file";
        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);
        int beforeNodeCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        int afterNodeCount = getNodeCount(graph);
        int afterEdgeCount = getEdgeCount(graph);

        /**
         * We should have afterwards:
         *  - 881 new concepts
         *  - 1386 new descriptions (1 per prefLabel)
         *  - 1 new Event
         *  - 882 new event links (1 for user, 1 per concept)
         */
        assertEquals(beforeNodeCount + 881 + 1386 + 1 + 882, afterNodeCount);

        // check narrower and broader...
        for (Concept c : graph.frameVertices(
                graph.getVertices(Ontology.IDENTIFIER_KEY, "?tema=511"), Concept.class)) {
            assertEquals(1, Iterables.size(c.getNarrowerConcepts()));
        }

        for (Concept c : graph.frameVertices(
                graph.getVertices(Ontology.IDENTIFIER_KEY, "?tema=512"), Concept.class)) {
            assertEquals(2, Iterables.size(c.getBroaderConcepts()));
        }

        // Check permission scopes
        for (AccessibleEntity e : log.getAction().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }

        // Check idempotency
        ImportLog log2 = importer
                .importFile(
                        ClassLoader.getSystemResourceAsStream(SKOS_FILE), logMessage);
        assertFalse(log2.hasDoneWork());
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(afterEdgeCount, getEdgeCount(graph));
    }


    // TODO export to RDF
    public class SkosCVocExporter {

        /**
         * Simple 'xml text' printing to generate RDF output
         * Note, we don't check if any value needs to be escaped for proper XML
         *
         * @param out
         * @param vocabulary
         */
        public void printRdf(final PrintStream out, final Vocabulary vocabulary) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            // rdfStart
            out.println("<rdf:RDF");
            out.println(" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"");
            out.println(" xmlns:dct=\"http://purl.org/dc/terms/\"");
            out.println(" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
            out.println(" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"");
            out.println(">");

            String vId = vocabulary.getIdentifier();
            // for all concepts
            for (Concept concept : vocabulary.getConcepts()) {
                // conceptStart
                String cId = concept.getIdentifier();
                out.println("<rdf:Description rdf:about=\"" + cId + "\">");
                out.println("  <rdf:type rdf:resource=\"http://www.w3.org/2004/02/skos/core#Concept\"/>");
                out.println("  <skos:inScheme rdf:resource=\"" + vId + "\"/>");

                // conceptDescriptions
                for (Description description : concept.getDescriptions()) {
                    String language = description.getLanguageOfDescription();
                    Vertex cdVertex = description.asVertex();
                    // prefLabel (one and only one)
                    String prefLabel = (String) cdVertex.getProperty(Ontology.PREFLABEL);
                    out.println("  <skos:prefLabel xml:lang=\"" + language + "\">" + prefLabel + "</skos:prefLabel>");

                    // other stuff as well
                    Object obj = cdVertex.getProperty(Ontology.CONCEPT_ALTLABEL);
                    if (obj != null) {
                        String[] altLabels = (String[]) obj;
                        for (String altLabel : altLabels) {
                            out.println("  <skos:altLabel xml:lang=\"" + language + "\">" + altLabel + "</skos:altLabel>");
                        }
                    }
                    obj = cdVertex.getProperty(Ontology.CONCEPT_SCOPENOTE);
                    if (obj != null) {
                        String[] scopeNotes = (String[]) obj;
                        for (String scopeNote : scopeNotes) {
                            out.println("  <skos:scopeNote xml:lang=\"" + language + "\">" + scopeNote + "</skos:altLabel>");
                        }
                    }
                    obj = cdVertex.getProperty(Ontology.CONCEPT_DEFINITION);
                    if (obj != null) {
                        String[] definitions = (String[]) obj;
                        for (String definition : definitions) {
                            out.println("  <skos:definition xml:lang=\"" + language + "\">" + definition + "</skos:altLabel>");
                        }
                    }

                }

                // broader
                for (Concept bc : concept.getBroaderConcepts()) {
                    out.println("  <skos:broader rdf:resource=\"" + bc.getIdentifier() + "\"/>");
                }
                // narraower
                for (Concept nc : concept.getNarrowerConcepts()) {
                    out.println("  <skos:narrower rdf:resource=\"" + nc.getIdentifier() + "\"/>");
                }
                // related
                for (Concept rc : concept.getRelatedConcepts()) {
                    out.println("  <skos:related rdf:resource=\"" + rc.getIdentifier() + "\"/>");
                }
                // related, reverse direction also
                for (Concept rc : concept.getRelatedByConcepts()) {
                    out.println("  <skos:related rdf:resource=\"" + rc.getIdentifier() + "\"/>");
                }

                // conceptEnd
                out.println("</rdf:Description>");
            }

            // rdfEnd
            out.println("</rdf:RDF>");
        }

    }
}
