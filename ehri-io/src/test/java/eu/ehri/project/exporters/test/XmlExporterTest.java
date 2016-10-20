package eu.ehri.project.exporters.test;

import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Before;


public class XmlExporterTest extends AbstractImporterTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper.setInitializing(false)
                .loadTestData(getClass()
                        .getClassLoader().getResourceAsStream("exportdata.yaml"));
    }
}
