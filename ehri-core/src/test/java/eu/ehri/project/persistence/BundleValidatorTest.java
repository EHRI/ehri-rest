package eu.ehri.project.persistence;

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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BundleValidatorTest extends AbstractFixtureTest {

    @Test
    public void testValidateForCreate() throws Exception {
        Bundle test = Bundle.fromData(TestData.getTestDocBundle());
        BundleValidator bundleValidator = new BundleValidator(manager, Lists.newArrayList());
        bundleValidator.validateForCreate(test);
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