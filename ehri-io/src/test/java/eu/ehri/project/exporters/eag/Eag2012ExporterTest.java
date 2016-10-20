/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.exporters.eag;

import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.importers.eag.EagHandler;
import eu.ehri.project.importers.eag.EagImporter;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.Repository;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import static eu.ehri.project.test.XmlTestHelpers.assertXPath;
import static eu.ehri.project.test.XmlTestHelpers.parseDocument;
import static eu.ehri.project.test.XmlTestHelpers.validatesSchema;


public class Eag2012ExporterTest extends XmlExporterTest {

    @Test
    public void testExport1() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        testExport(r1, "eng");
    }

    @Test
    public void testImportExport1() throws Exception {
        Country nl = manager.getEntity("nl", Country.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream("eag-2896.xml");
        SaxImportManager importManager = new SaxImportManager(graph, nl, validUser,
                EagImporter.class, EagHandler.class);
        importManager.importInputStream(ios, "Text EAG import/export");
        Repository repo = graph.frame(getVertexByIdentifier(graph, "NL-002896"), Repository.class);
        testExport(repo, "eng");
    }

    @Test
    public void testExportWithComprehensiveFixture() throws Exception {
        Repository test = manager.getEntity("nl-000001", Repository.class);
        Document doc = parseDocument(testExport(test, "eng"));
        assertXPath(doc, "NL-000001", "//eag/control/recordId");
        assertXPath(doc, "nl-000001", "//eag/control/otherRecordId");
        assertXPath(doc, "2013-09-09",
                "//eag/control/maintenanceHistory/maintenanceEvent/eventDateTime");
        assertXPath(doc, "Institution Example", "//eag/archguide/identity/autform");
        assertXPath(doc, "Exemple institution", "//eag/archguide/identity/parform[1]");
        assertXPath(doc, "Institution Beispiel", "//eag/archguide/identity/parform[2]");
        assertXPath(doc, "מוסד דוגמא", "//eag/archguide/identity/parform[3]");
        assertXPath(doc, "EI", "//eag/archguide/identity/parform[4]");
        assertXPath(doc, "001122",
                "//eag/archguide/desc/repositories/repository/location/municipalityPostalcode");
        assertXPath(doc, "1 Example Street",
                "//eag/archguide/desc/repositories/repository/location/street");
        assertXPath(doc, "00 12 3456789",
                "//eag/archguide/desc/repositories/repository/telephone");
        assertXPath(doc, "00 12 3456789",
                "//eag/archguide/desc/repositories/repository/fax");
        assertXPath(doc, "test@example.com",
                "//eag/archguide/desc/repositories/repository/email/@href");
        assertXPath(doc, "http://www.example.nl",
                "//eag/archguide/desc/repositories/repository/webpage[1]/@href");
        assertXPath(doc, "http://www.example.nl/en",
                "//eag/archguide/desc/repositories/repository/webpage[2]/@href");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/repositorhist/descriptiveNote/p");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/buildinginfo/building/descriptiveNote/p");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/holdings/descriptiveNote/p");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/timetable/opening");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/access/termsOfUse");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/accessibility");
        assertXPath(doc, "Example text\n",
                "//eag/archguide/desc/repositories/repository/" +
                        "services/searchroom/researchServices/descriptiveNote/p");
    }

    private String testExport(Repository repository, String lang) throws Exception {
        Eag2012Exporter exporter = new Eag2012Exporter(api(validUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(repository, baos, lang);
        String xml = baos.toString("UTF-8");
        //System.out.println(xml);
        isValidEag(xml);
        return xml;
    }

    private void isValidEag(String eagXml) throws IOException, SAXException {
        validatesSchema(eagXml, "eag_2012.xsd");
    }
}