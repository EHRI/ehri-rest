package eu.ehri.project.importers.test;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.SkosCoreCvocImporter;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.views.impl.Query;

public class SkosCoreCvocImporterTest extends AbstractImporterTest { //AbstractFixtureTest {
	// Note we only can do RSF
    protected final String SKOS_FILE = "ehri-skos.rdf";//"broaderskos.xml";//"skos.rdf";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        //printGraph(graph);
        
        // How many new nodes will have been created? We should have
        // Yet we've only created 1 *logical* item...
        //assertEquals(1, log.getSuccessful());   
        
        /* check if broader is set
        String skosConceptId = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=512";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);
        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).list(
                AccessibleEntity.IDENTIFIER_KEY, skosConceptId, validUser));
        for (Concept c: list) {
        	System.out.println("Retrieved after import; Concept: " + c.getIdentifier());
        	Iterable<Concept> bcList = c.getBroaderConcepts();
        	 for (Concept bc: bcList) {
        		 System.out.println("-> broader Concept: " + bc.getIdentifier());
        	 }
        }
        */
        
        // get a top concept
        //String skosConceptId = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=511";
        String skosConceptId = "http://ehri01.dans.knaw.nl/tematres/vocab/?tema=330";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);
        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).list(
                AccessibleEntity.IDENTIFIER_KEY, skosConceptId, validUser));
        // and print the tree
        printConceptTree(System.out, list.get(0));
    }

    
    // Print a Concept Tree from a 'top' concept down into all narrower concepts
    public void printConceptTree(final PrintStream out, Concept c) {
    	printConceptTree(out, c, 0, "");
    }
    public void printConceptTree(final PrintStream out, Concept c, int depth, String indent) {
    	if (depth > 100) {
    		out.println("STOP RECURSION");
    		return;
    	}

    	//for(int i = 0; i<depth; i++) out.print("  "); // indentation
    	out.print(indent);
    	
    	out.print("["+ c.getIdentifier()+"]");
    	Iterable<Description> descriptions = c.getDescriptions();
    	for (Description d: descriptions) {
    		String lang = d.getLanguageOfDescription();
    		String prefLabel = (String)d.asVertex().getProperty(ConceptDescription.PREFLABEL); // can't use the getPrefLabel() !
    		out.print(", \"" + prefLabel + "\"(" + lang+ ")");
    	}
    	out.println("");
        indent += ".   ";// the '.' improves readability, but the whole printing could be improved
    	        
    	for (Concept nc: c.getNarrowerConcepts()) {
    		printConceptTree(out, nc, ++depth, indent); // recursive call
    	}
    }
}
