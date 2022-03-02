/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.exporters.eac;

import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.eac.EacHandler;
import eu.ehri.project.importers.eac.EacImporter;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static eu.ehri.project.test.XmlTestHelpers.assertXPath;
import static eu.ehri.project.test.XmlTestHelpers.parseDocument;
import static eu.ehri.project.test.XmlTestHelpers.validatesSchema;


public class Eac2010ExporterTest extends XmlExporterTest {
    @Test
    public void testExport1() throws Exception {
        HistoricalAgent agent = manager.getEntity("a1", HistoricalAgent.class);
        testExport(agent, "eng");
    }

    @Test
    public void testImportExport1() throws Exception {
        AuthoritativeSet auths = manager.getEntity("auths", AuthoritativeSet.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream("abwehr.xml");
        String logMessage = "Test EAC import/export";
        SaxImportManager.create(graph, auths, validUser,
                EacImporter.class, EacHandler.class, ImportOptions.properties("eac.properties"))
                .importInputStream(ios, logMessage);
        HistoricalAgent repo = graph.frame(getVertexByIdentifier(graph, "381"), HistoricalAgent.class);
        String xml = testExport(repo, "eng");
        Document doc = parseDocument(xml);
        assertXPath(doc, logMessage,
                "//eac-cpf/control/maintenanceHistory/maintenanceEvent[3]/eventDescription");
    }

    @Test
    public void testExportWithComprehensiveFixture() throws Exception {
        HistoricalAgent test = manager.getEntity("auths-000001", HistoricalAgent.class);
        Document doc = parseDocument(testExport(test, "eng"));
        assertXPath(doc, "auths-000001", "//eac-cpf/control/recordId");
        assertXPath(doc, "000001", "//eac-cpf/control/otherRecordId");
        assertXPath(doc, "2013-09-09",
                "//eac-cpf/control/maintenanceHistory/maintenanceEvent/eventDateTime");
        assertXPath(doc, "000001", "//eac-cpf/cpfDescription/identity/entityId");
        assertXPath(doc, "person", "//eac-cpf/cpfDescription/identity/entityType");
        assertXPath(doc, "Historical Agent Example", "//eac-cpf/cpfDescription/identity/nameEntry/part");
        assertXPath(doc, "HAE", "//eac-cpf/cpfDescription/identity/nameEntry[2]/part");
        assertXPath(doc, "Historique Agent Exemple",
                "//eac-cpf/cpfDescription/identity/nameEntryParallel/nameEntry[2]/part");
        assertXPath(doc, "Historische Agent- Beispiel",
                "//eac-cpf/cpfDescription/identity/nameEntryParallel/nameEntry[3]/part");
        assertXPath(doc, "היסטורי סוכן דוגמא",
                "//eac-cpf/cpfDescription/identity/nameEntryParallel/nameEntry[4]/part");

        assertXPath(doc, "1940",
                "//eac-cpf/cpfDescription/description/existDates/dateRange/fromDate");
        assertXPath(doc, "1945",
                "//eac-cpf/cpfDescription/description/existDates/dateRange/toDate");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/existDates/descriptiveNote/p");
        assertXPath(doc, "Example text",
                "//eac-cpf/cpfDescription/description/place/placeEntry");
        assertXPath(doc, "More example text",
                "//eac-cpf/cpfDescription/description/place[2]/placeEntry");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/legalStatus/term");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/function/term");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/occupation/term");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/structureOrGenealogy/p");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/generalContext/p");
        assertXPath(doc, "Example text\n",
                "//eac-cpf/cpfDescription/description/biogHist/p");
    }

    private String testExport(HistoricalAgent agent, String lang) throws Exception {
        Eac2010Exporter exporter = new Eac2010Exporter(api(validUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(agent, baos, lang);
        String xml = baos.toString("UTF-8");
        //System.out.println(xml);
        isValidEac(xml);
        return xml;
    }

    private void isValidEac(String eacXml) throws IOException, SAXException {
        validatesSchema(eacXml, "eac.xsd");
    }
}