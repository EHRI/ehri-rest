/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class UkrainianUnitImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesImporterTest.class);
    protected final String SINGLE_EAD = "ukraine_small.csv";

    @Test
    public void testImportItemsT() throws Exception {
        Bundle ukrainianBundle = new Bundle(EntityClass.COUNTRY)
                                .withDataValue(Country.COUNTRY_CODE, "ua");
        Bundle repoBundle = new Bundle(EntityClass.REPOSITORY).withDataValue(Ontology.IDENTIFIER_KEY, "ua-3311");
        Bundle repoDescBundle = new Bundle(EntityClass.REPOSITORY_DESCRIPTION)
                .withDataValue(Ontology.NAME_KEY, "Центральний державний архів вищих органів влади і управління України")
                .withDataValue("otherFormsOfName", "Central State Archive of Supreme Bodies of Power and Government of Ukraine")
                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "uk");
        repoBundle = repoBundle.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, repoDescBundle);
        repoBundle = repoBundle.withRelation(Ontology.REPOSITORY_HAS_COUNTRY, ukrainianBundle);
        Repository repo = new CrudViews<Repository>(graph, Repository.class).create(repoBundle, validUser);

        int count = getNodeCount(graph);
        
        final String logMessage = "Importing some Ukrainian records";
        XmlImportProperties p = new XmlImportProperties("ukraine.properties");
        assertTrue(p.containsProperty("identifier"));
        assertTrue(p.containsProperty("level_of_description"));
        assertTrue(p.containsProperty("project_judaica"));
        
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, repo, validUser, UkrainianUnitImporter.class).importFile(ios, logMessage);

        /*
         * 17 DocumentaryUnits
         * 17 + 3 DocumentsDescription (there are 3 desc's with 2 languages)
         * 17 + 3 DatePeriods
         * 18 more import Event links (1 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count+76, getNodeCount(graph));
        printGraph(graph);
//        assertEquals(voccount + 8, toList(authoritativeSet.getAuthoritativeItems()).size());
       
    }
}
