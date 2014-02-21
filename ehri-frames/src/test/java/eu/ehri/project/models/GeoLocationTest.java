package eu.ehri.project.models;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.utils.BundleUtils;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class GeoLocationTest extends AbstractFixtureTest {
    @Test
    public void testSerialization() throws Exception {
        // Goodness me this is a faff - improve API!
        Bundle bundle = Bundle.fromData(TestData.getTestAgentBundle());
        double lat = 50.0302;
        double lon = 19.17612;
        Bundle geoLoc = new Bundle.Builder(EntityClass.GEO_LOCATION)
                .addDataValue(Ontology.LATITUDE, lat)
                .addDataValue(Ontology.LONGITUDE, lon)
                .build();

        Bundle withGeo = BundleUtils
                .setBundle(bundle, "describes[0]/hasAddress[0]/hasLocation[-1]", geoLoc);
        BundleDAO persister = new BundleDAO(graph);
        Repository repository = persister.create(withGeo, Repository.class);
        RepositoryDescription desc = repository.getRepositoryDescriptions()
                .iterator().next();
        Address address = desc.getAddresses().iterator().next();
        GeoLocation geoLocation = address.getGeoLocations().iterator().next();
        assertNotNull(geoLocation);
        assertEquals(Double.valueOf(lat), geoLocation.getLatitude());
        assertEquals(Double.valueOf(lon), geoLocation.getLongitude());
    }

    @Test(expected = ValidationError.class)
    public void testValidation() throws Exception {
        // Add a bundle but miss out vital longitude value...
        Bundle bundle = Bundle.fromData(TestData.getTestAgentBundle());
        double lat = 50.0302;
        Bundle geoLoc = new Bundle.Builder(EntityClass.GEO_LOCATION)
                .addDataValue(Ontology.LATITUDE, lat)
                .build();

        Bundle withGeo = BundleUtils
                .setBundle(bundle, "describes[0]/hasAddress[0]/hasLocation[-1]", geoLoc);
        BundleDAO persister = new BundleDAO(graph);
        persister.create(withGeo, Repository.class);
        fail("Creating bundle with invalid geo location " +
                "should throw a ValidationError");
    }
}
