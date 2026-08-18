/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.exporters.ead;

import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.Repository;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import static eu.ehri.project.test.XmlTestHelpers.*;
import static org.junit.Assert.assertTrue;


public class Ead3ExporterTest extends XmlExporterTest {

    private static final ResourceBundle i18n = ResourceBundle.getBundle(Ead2002Exporter.class.getName());

    @Test
    public void testExport1() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        String xml = testExport(c1, "eng");
        Document doc = parseDocument(xml);
        assertXPath(doc, "auths", "//controlaccess/subject/@source");
        assertXPath(doc, "a1", "//controlaccess/subject/@identifier");
    }

    @Test
    public void testExport2() throws Exception {
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        String xml = testExport(c4, "eng");
        Document doc = parseDocument(xml);
        assertXPath(doc, "A link indicated that r4 is the original location of c4",
                "//ead/archdesc/originalsloc/p");
    }

    @Test
    public void testImportExport1() throws Exception {
        Repository repo = manager.getEntity("r1", Repository.class);
        testImportExport(repo, "hierarchical-ead.xml", "Ctop_level_fonds", "eng");
    }

    @Test
    public void testImportExport2() throws Exception {
        Repository repo = manager.getEntity("r1", Repository.class);
        String xml = testImportExport(repo, "comprehensive-ead-ead3.xml",
                "Resource (call) |||.Ident (num) |||", "eng");
        // System.out.println(xml);
        Document doc = parseDocument(xml);
        String pidPrefix = config.getString("io.pids.prefix");
        assertXPath(doc, readResourceFileAsString("export-boilerplate.txt"),
                "//ead/control/maintenancehistory/maintenanceevent[1]/eventdescription/text()");
        assertXPath(doc, String.format("Testing import/export [%s]", i18n.getString("ingest")),
                "//ead/control/maintenancehistory/maintenanceevent[2]/eventdescription/text()");
        assertXPath(doc, "eng",
                "//ead/control/languagedeclaration/language/@langcode");
        assertXPath(doc, "Local",
                "//ead/control/conventiondeclaration/descriptivenote/p/text()");
        assertXPath(doc, "NIOD Description",
                "//ead/control/filedesc/publicationstmt/publisher/text()");
        assertXPath(doc, "NIOD Description",
                "//ead/archdesc/did/repository/corpname/part/text()");
        assertXPath(doc, "eng", "//ead/archdesc/did/langmaterial/languageset/language/@langcode");
        assertXPath(doc, "Latn", "//ead/archdesc/did/langmaterial/languageset/script/@scriptcode");
        assertXPath(doc, "Scope and contents note content no label |||\n\n" +
                        "Scope and contents note content |||",
                "//ead/archdesc/scopecontent/p/text()");
        assertXPath(doc, "Separated materials note content no label |||",
                "//ead/archdesc/separatedmaterial[2]/p/text()");
        assertXPath(doc, "Series I |||",
                "//ead/archdesc/dsc/c01/did/unitid/text()");
        assertXPath(doc, "Folder 3 |||",
                "//ead/archdesc/dsc/c01[3]/c02[2]/did/unitid/text()");
        assertXPath(doc, pidPrefix + "pid-folder-3",
                "//ead/archdesc/dsc/c01[3]/c02[2]/did/unitid[2]/text()");
        assertXPath(doc, "Processing information note no label |||\n\n" +
                        "Processing information note content |||",
                "//ead/archdesc/processinfo[@encodinganalog='3.7.1']/p");
        assertXPath(doc, "2000",
                "//ead/archdesc/processinfo[@encodinganalog='3.7.3']/p/date");
        assertXPath(doc, "Source information |||",
                "//ead/archdesc/processinfo/p/ref");
        assertXPath(doc, "1989",
                "//ead/archdesc/did/unitdatestructured/daterange/fromdate");
        assertXPath(doc, "1999",
                "//ead/archdesc/did/unitdatestructured/daterange/todate");
        assertXPath(doc, "Bowers, Kate, 1963- |||", "//ead/archdesc/did/origination[1]/persname/part/text()");
        assertXPath(doc, "Test|||, Name|||, Ms.|||, Number|||, Suffix|||, Title|||, (Fuller form|||), 1880-1980|||, qualifier|||",
                "//ead/archdesc/did/origination[2]/persname/part/text()");
    }

    @Test
    public void testExportWithComprehensiveFixture() throws Exception {
        DocumentaryUnit test = manager.getEntity("nl-000001-1", DocumentaryUnit.class);
        String xml = testExport(test, "eng");
        //System.out.println(xml);
        Document doc = parseDocument(xml);
        assertXPath(doc, "nl-000001-1", "/ead/control/recordid");
        assertXPath(doc, readResourceFileAsString("export-boilerplate.txt"),
                "//ead/control/maintenancehistory/maintenanceevent[1]/eventdescription/text()");
        assertXPath(doc, "Example Documentary Unit 1",
                "//ead/control/filedesc/titlestmt/titleproper");
        assertXPath(doc, readResourceFileAsString("creationprocess-boilerplate.txt"),
                "//ead/control/filedesc/notestmt/controlnote/p");
        assertXPath(doc, "Institution Example",
                "//ead/control/filedesc/publicationstmt/publisher");
        assertXPath(doc, "Netherlands",
                "//ead/control/filedesc/publicationstmt/address/addressline[8]");
        assertXPath(doc, "Example text",
                "//ead/control/conventiondeclaration/descriptivenote/p[1]");
        assertXPath(doc, "eng", "//ead/control/languagedeclaration/language/@langcode");
        assertXPath(doc, "1", "//ead/archdesc/did/unitid");
        assertXPath(doc, "Example Documentary Unit 1", "//ead/archdesc/did/unittitle");
        assertXPath(doc, "Institution Example", "//ead/archdesc/did/repository/corpname/part");
        assertXPath(doc, "eng", "//ead/archdesc/did/langmaterial/languageset/language/@langcode");
        assertXPath(doc, "Latn", "//ead/archdesc/did/langmaterial/languageset/script/@scriptcode");
        assertXPath(doc, "ger", "//ead/archdesc/did/langmaterial/language/@langcode");
        assertXPath(doc, "Example text", "//ead/archdesc/did/langmaterial/descriptivenote/p");
        assertXPath(doc, "a", "//ead/archdesc/dsc/c01/did/unitid");
        assertXPath(doc, "i", "//ead/archdesc/dsc/c01/c02/did/unitid");
        assertXPath(doc, "Example text", "//ead/archdesc/scopecontent/p");
        assertXPath(doc, "Example text", "//ead/archdesc/arrangement/p");
        assertXPath(doc, "Example text", "//ead/archdesc/bibliography/p");
        assertXPath(doc, "Example text", "//ead/archdesc/altformavail/p");
        assertXPath(doc, "Example text", "//ead/archdesc/originalsloc/p");
        assertXPath(doc, "Example text", "//ead/archdesc/bioghist/p");
        assertXPath(doc, "Example text", "//ead/archdesc/accessrestrict/p");
        assertXPath(doc, "Example text", "//ead/archdesc/userestrict/p");
        assertXPath(doc, "Example text", "//ead/archdesc/accruals/p");
        assertXPath(doc, "Example text", "//ead/archdesc/acqinfo/p");
        assertXPath(doc, "Example text", "//ead/archdesc/appraisal/p");
        assertXPath(doc, "Example text", "//ead/archdesc/custodhist/p");
        assertXPath(doc, "Example text", "//ead/archdesc/phystech/p");
        assertXPath(doc, "Example text", "//ead/archdesc/odd/p");
        assertXPath(doc, "Example text", "//ead/archdesc/processinfo[@encodinganalog='3.7.1']/p");
        assertXPath(doc, "2000", "//ead/archdesc/processinfo[@encodinganalog='3.7.3']/p/date");
        assertXPath(doc, "Example text", "//ead/archdesc/processinfo[@localtype='Sources']/p/ref");
        assertXPath(doc, "Example Person 1", "//ead/archdesc/controlaccess/persname/part");
        assertXPath(doc, "Example Subject 1", "//ead/archdesc/controlaccess/subject/part");
        assertXPath(doc, "1939-1945", "//ead/archdesc/did/unitdate");
        assertXPath(doc, "1939-01-01", "//ead/archdesc/did/unitdatestructured/daterange/fromdate/@standarddate");
        assertXPath(doc, "1945-01-01", "//ead/archdesc/did/unitdatestructured/daterange/todate/@standarddate");
        // The c01 date period has week precision: ISO 8601 cannot distinguish a week from a
        // day, so the standarddate stays a full date and the precision is retained in @localtype
        assertXPath(doc, "1939-01-01", "//ead/archdesc/dsc/c01/did/unitdatestructured/daterange/fromdate/@standarddate");
        assertXPath(doc, "1945-01-01", "//ead/archdesc/dsc/c01/did/unitdatestructured/daterange/todate/@standarddate");
        assertXPath(doc, "week", "//ead/archdesc/dsc/c01/did/unitdatestructured/daterange/fromdate/@localtype");
        assertXPath(doc, "week", "//ead/archdesc/dsc/c01/did/unitdatestructured/daterange/todate/@localtype");
        // The c02 date period has year precision, so the standarddate is truncated to the year;
        // year precision needs no @localtype since the ISO date already conveys the granularity
        assertXPath(doc, "1939", "//ead/archdesc/dsc/c01/c02/did/unitdatestructured/daterange/fromdate/@standarddate");
        assertXPath(doc, "1945", "//ead/archdesc/dsc/c01/c02/did/unitdatestructured/daterange/todate/@standarddate");

    }

    @Test
    public void testDatePrecisionRoundTrip() throws Exception {
        Repository repo = manager.getEntity("r1", Repository.class);
        String xml = testImportExport(repo, "precision-ead3.xml", "prec-1", "eng");

        // The stored startDate/endDate are always widened to a full date, regardless
        // of the source @standarddate's precision - only the precision field, not the
        // date's width, should carry that information.
        DocumentaryUnit unit = graph.frame(getVertexByIdentifier(graph, "prec-1"), DocumentaryUnit.class);
        Iterable<DatePeriod> periods = unit.getDocumentDescriptions().iterator().next()
                .as(DocumentaryUnitDescription.class).getDatePeriods();
        assertTrue(toList(periods).stream().anyMatch(p -> "1939-04-01".equals(p.getStartDate())));

        Document doc = parseDocument(xml);
        // Quarter precision was carried explicitly via @localtype and is preserved.
        // The standarddate stays month-truncated (ISO 8601 has no quarter form).
        assertXPath(doc, "quarter",
                "//unitdatestructured/daterange/fromdate[@standarddate='1939-04']/@localtype");
        assertXPath(doc, "quarter",
                "//unitdatestructured/daterange/todate[@standarddate='1945-06']/@localtype");
        // Month precision was inferred from the granularity of the standardised date:
        // if it had not been imported, the re-export would default to day precision
        // and emit "1940-01-01" rather than "1940-01". The month daterange carries no
        // @localtype, which distinguishes it from the quarter one above.
        assertXPath(doc, "1940-01",
                "//unitdatestructured/daterange/fromdate[not(@localtype)]/@standarddate");
        assertXPath(doc, "1944-12",
                "//unitdatestructured/daterange/todate[not(@localtype)]/@standarddate");
    }

    private String testExport(DocumentaryUnit unit, String lang) throws Exception {
        Ead3Exporter exporter = new Ead3Exporter(api(adminUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(unit, baos, lang);
        String xml = baos.toString("UTF-8");
//        System.out.println(xml);
        isValidEad(xml);
        return xml;
    }

    private String testImportExport(Repository repository, String resourceName,
            String topLevelIdentifier, String lang) throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream(resourceName);
        SaxImportManager.create(graph, repository, adminUser,
                EadImporter.class, EadHandler.class, ImportOptions.properties("ead3.properties"))
                .withPreCallback(getPidGeneratorCallback())
                .importInputStream(ios, "Testing import/export");
        DocumentaryUnit fonds = graph.frame(
                getVertexByIdentifier(graph, topLevelIdentifier), DocumentaryUnit.class);
        Ead3Exporter exporter = new Ead3Exporter(api(adminUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(fonds, baos, lang);
        String xml = baos.toString("UTF-8");
        isValidEad(xml);
        return xml;
    }

    private void isValidEad(String eadXml) throws IOException, SAXException {
        validatesSchema(eadXml, "ead3.xsd");
    }
}