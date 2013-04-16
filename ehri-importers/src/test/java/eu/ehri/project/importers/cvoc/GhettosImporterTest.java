/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.cvoc;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.views.Query;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author linda
 */
public class GhettosImporterTest extends AbstractImporterTest{
    protected final String SKOS_FILE = "ghettos.rdf";
   
    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);
        
        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

//        printGraph(graph);
        /*  How many new nodes will have been created? We should have
        * 2 more Concepts
       	* 4 more ConceptDescription
	* 3 more import Event links (2 for every Unit, 1 for the User)
        * 1 more import Event
        */
        assertEquals(count + 10, getNodeCount(graph));
        // get a top concept

        String skosConceptId = "http://ehri01.dans.knaw.nl/ghettos/0";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);
        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).list(
                IdentifiableEntity.IDENTIFIER_KEY, skosConceptId, validUser));
        // and print the tree
        printConceptTree(System.out, list.get(0));
        
        // output RDF
//        new SkosCVocExporter().printRdf(System.out, vocabulary);
        
        /*
        // NOW add again to another vocabulary (Yep, doesn't work!)
        vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
        ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        log = importer.importFile(ios, logMessage);
		*/
    }

    
    // Print a Concept Tree from a 'top' concept down into all narrower concepts
    public void printConceptTree(final PrintStream out, Concept c) {
    	printConceptTree(out, c, 0, "");
    }
    public void printConceptTree(final PrintStream out, Concept c, int depth, String indent) {
    	if (depth > 100) {
    		out.println("STOP RECURSION, possibly cyclic 'tree'");
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
    	// TODO Print related concept ids?
    	for (Concept related: c.getRelatedConcepts()) {
    		out.print("["+ related.getIdentifier() + "]");
    	}
    	for (Concept relatedBy: c.getRelatedByConcepts()) {
    		out.print("["+ relatedBy.getIdentifier() + "]");
    	}
    	
    	out.println("");// end of concept
    	
        indent += ".   ";// the '.' improves readability, but the whole printing could be improved        
    	for (Concept nc: c.getNarrowerConcepts()) {
    		printConceptTree(out, nc, ++depth, indent); // recursive call
    	}
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
        	for(Concept concept : vocabulary.getConcepts()) {
        	  // conceptStart
        	  String cId = concept.getIdentifier();
        	  out.println("<rdf:Description rdf:about=\"" + cId + "\">");
        	  out.println("  <rdf:type rdf:resource=\"http://www.w3.org/2004/02/skos/core#Concept\"/>");
        	  out.println("  <skos:inScheme rdf:resource=\"" +  vId +  "\"/>");
        	  
        	  // conceptDescriptions
        	  for(Description description : concept.getDescriptions()) {
        		  String language = description.getLanguageOfDescription();
        		  Vertex cdVertex = description.asVertex();
        		  // prefLabel (one and only one)
        		  String prefLabel = (String)cdVertex.getProperty(ConceptDescription.PREFLABEL);
        		  out.println("  <skos:prefLabel xml:lang=\""+ language +"\">"+ prefLabel +"</skos:prefLabel>");
        	  
        		  // other stuff as well
        		  Object obj = cdVertex.getProperty(ConceptDescription.ALTLABEL); 
        		  if (obj != null) {
	        		  String[] altLabels = (String[])obj; 
	        		  for (String altLabel : altLabels) {
	            		  out.println("  <skos:altLabel xml:lang=\""+ language +"\">"+ altLabel +"</skos:altLabel>");        			  
	        		  }
        		  }
        		  obj = cdVertex.getProperty(ConceptDescription.SCOPENOTE); 
        		  if (obj != null) {
	        		  String[] scopeNotes = (String[])obj; 
	        		  for (String scopeNote : scopeNotes) {
	            		  out.println("  <skos:scopeNote xml:lang=\""+ language +"\">"+ scopeNote +"</skos:altLabel>");        			  
	        		  }
        		  }
        		  obj = cdVertex.getProperty(ConceptDescription.DEFINITION); 
        		  if (obj != null) {
	        		  String[] definitions = (String[])obj; 
	        		  for (String definition : definitions) {
	            		  out.println("  <skos:definition xml:lang=\""+ language +"\">"+ definition +"</skos:altLabel>");        			  
	        		  }
        		  }
        		  
        	  }
        	  
        	  // broader
        	  for(Concept bc : concept.getBroaderConcepts()) {
        		  out.println("  <skos:broader rdf:resource=\"" + bc.getIdentifier() + "\"/>");
        	  }
        	  // narraower
         	  for(Concept nc : concept.getNarrowerConcepts()) {
        		  out.println("  <skos:narrower rdf:resource=\"" + nc.getIdentifier() + "\"/>");
        	  }
        	  // related
          	  for(Concept rc : concept.getRelatedConcepts()) {
        		  out.println("  <skos:related rdf:resource=\"" + rc.getIdentifier() + "\"/>");
        	  }
          	  // related, reverse direction also
           	  for(Concept rc : concept.getRelatedByConcepts()) {
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
