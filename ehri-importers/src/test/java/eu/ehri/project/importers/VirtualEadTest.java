/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;
/**
 *
 * @author linda
 */
public class VirtualEadTest extends AbstractImporterTest{
    private static final Logger logger = LoggerFactory.getLogger(VirtualEadTest.class);
    protected final String TEST_REPO = "mike";
    protected final String XMLFILE = "wp2_virtualcollection.xml";
    private static final String REPO1= "il-002777";
    private static final String REPO2= "cz-002302";
    private static final String UNIT1 ="wp2-bt";
    private static final String UNIT2 ="vzpomínky pro EHRI";
    
    private static final String ARCHDESC = "EHRI Terezin Research Guide";
    private static final String C01_VirtualReferrer = REPO1+"-"+UNIT1;
    private static final String C01_VirtualLevel = "vc-tm";
    private static final String C02 = REPO2+"-"+UNIT2;
    
    Repository repository1, repository2;
    DocumentaryUnit unit1, unit2;
    
int origCount=0;

@Test
public void setStageTest() throws PermissionDenied, ValidationError, IntegrityError{
     setStage();
     assertEquals(REPO1, repository1.getIdentifier());
     assertEquals(UNIT1, unit1.getIdentifier());
}

    @Test
    public void virtualUnitTest() throws ItemNotFound, IOException, ValidationError, InputParseError, PermissionDenied, IntegrityError {
        
        setStage();
        
        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing a part of a NIOD EAD";

        origCount = getNodeCount(graph);
        
 // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE);
        @SuppressWarnings("unused")
	ImportLog log = new SaxImportManager(graph, agent, validUser, VirtualEadImporter.class, VirtualEadHandler.class, new XmlImportProperties("vc.properties")).importFile(ios, logMessage);
         // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 4 more VirtualUnits (archdesc, 3 children)
       	// - 2 more DocumentDescription
        // - 5 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        int newCount = origCount + 12; 
        assertEquals(newCount, getNodeCount(graph));
    }

    private void setStage() throws PermissionDenied, ValidationError, IntegrityError {
        Bundle repo1Bundle = new Bundle(EntityClass.REPOSITORY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, REPO1);
        Bundle repo2Bundle = new Bundle(EntityClass.REPOSITORY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, REPO2);
        Bundle documentaryUnit1Bundle = new Bundle(EntityClass.DOCUMENTARY_UNIT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT1);
        Bundle documentDescription1Bundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION)
                                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT1+"desc")
                                .withDataValue(Ontology.NAME_KEY, UNIT1+"title")
                                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "eng");
        
        Bundle documentaryUnit2Bundle = new Bundle(EntityClass.DOCUMENTARY_UNIT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT2);
        Bundle documentDescription2Bundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION)
                                .withDataValue(Ontology.IDENTIFIER_KEY, UNIT2+"desc")
                                .withDataValue(Ontology.NAME_KEY, UNIT2+"title")
                                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "eng");

        repository1 = new CrudViews<Repository>(graph, Repository.class).create(repo1Bundle, validUser);
        repository2 = new CrudViews<Repository>(graph, Repository.class).create(repo2Bundle, validUser);
        
        documentaryUnit1Bundle = documentaryUnit1Bundle.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, documentDescription1Bundle);
        unit1 = new CrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class).create(documentaryUnit1Bundle, validUser);
        unit1.setRepository(repository1);
        
        documentaryUnit2Bundle = documentaryUnit2Bundle.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, documentDescription2Bundle);
        unit2 = new CrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class).create(documentaryUnit2Bundle, validUser);
        unit2.setRepository(repository2);

    }
    
}
