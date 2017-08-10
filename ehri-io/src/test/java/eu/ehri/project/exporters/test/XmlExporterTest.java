package eu.ehri.project.exporters.test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Before;


public class XmlExporterTest extends AbstractImporterTest {

    protected static final Config config = ConfigFactory.load();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper.setInitializing(false)
                .loadTestData(getClass()
                        .getClassLoader().getResourceAsStream("exportdata.yaml"));
    }
}
