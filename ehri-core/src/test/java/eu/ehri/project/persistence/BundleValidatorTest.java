package eu.ehri.project.persistence;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistence.utils.DataUtils;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class BundleValidatorTest extends AbstractFixtureTest {

    @Test
    public void testValidateForCreate() throws Exception {
        Bundle test = Bundle.fromData(TestData.getTestDocBundle());
        BundleValidator bundleValidator = new BundleValidator(manager, Lists.newArrayList());
        bundleValidator.validateForCreate(test);
    }

    @Test
    public void testValidateIntegrity() throws Exception {

        Bundle test = DataUtils.setItem(
                Bundle.fromData(TestData.getTestDocBundle()),
                "describes[-1]", Bundle.Builder.withClass(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                        .addData(ImmutableMap.of(
                                Ontology.IDENTIFIER_KEY, "someid-01",
                                Ontology.NAME_KEY, "Description with duplicate identifier",
                                Ontology.LANGUAGE_OF_DESCRIPTION, "eng"
                        )).build());

        BundleValidator bundleValidator = new BundleValidator(manager, Lists.newArrayList());
        try {
            bundleValidator.validateForCreate(test);
            fail("Bundle validation should have failed with duplicate ID error");
        } catch (ValidationError e) {
            List<String> errs = DataUtils.get(e.getErrorSet(), "describes[1]/languageCode");
            assertEquals(1, errs.size());
            assertThat(errs.get(0), containsString("Value 'eng-someid_01' exists and must be unique"));
        }
    }

    @Test
    public void testValidateIntegrity2() throws Exception {
        // Check a weird case where the ID of a dependent item generates the same as
        // one elsewhere in the bundle...
        Bundle test = DataUtils.setItem(
                Bundle.fromData(TestData.getTestDocBundle()),
                "describes[-1]", Bundle.Builder.withClass(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                        .setId("someid_01") // Uh-oh!
                        .addData(ImmutableMap.of(
                                Ontology.IDENTIFIER_KEY, "ok",
                                Ontology.NAME_KEY, "Description with duplicate identifier",
                                Ontology.LANGUAGE_OF_DESCRIPTION, "eng"
                        )).build());

        BundleValidator bundleValidator = new BundleValidator(manager, Lists.newArrayList());
        try {
            bundleValidator.validateForCreate(test);
            fail("Bundle validation should have failed with duplicate ID error");
        } catch (ValidationError e) {
            List<String> errs = DataUtils.get(e.getErrorSet(), "id");
            assertEquals(1, errs.size());
            assertThat(errs.get(0), containsString("Duplicate ID: someid_01"));
        }
    }

    @Test
    public void testValidateForCreateWithError() throws Exception {
        Bundle test = DataUtils.setItem(
                Bundle.fromData(TestData.getTestDocBundle()),
                "describes[0]/relatesTo[-1]",
                Bundle.Builder.withClass(EntityClass.ACCESS_POINT)
                        .addDataValue(Ontology.NAME_KEY, "Test")
                        .addDataValue(Ontology.ACCESS_POINT_TYPE, "bad")
                        .build());
        BundleValidator bundleValidator = new BundleValidator(manager, Lists.newArrayList());

        try {
            bundleValidator.validateForCreate(test);
            fail("Bundle validation should have failed");
        } catch (ValidationError e) {
            List<String> errs = DataUtils.get(e.getErrorSet(),
                    "describes[0]/relatesTo[0]/type");
            assertEquals(1, errs.size());
            assertThat(errs.get(0), containsString("Property value must be one of"));
        }
    }
}