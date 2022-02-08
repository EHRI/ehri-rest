package eu.ehri.project.oaipmh;

import com.typesafe.config.ConfigFactory;
import eu.ehri.project.api.Api;
import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.exporters.xml.IndentingXMLStreamWriter;
import eu.ehri.project.oaipmh.errors.OaiPmhError;
import eu.ehri.project.test.XmlTestHelpers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLOutputFactory;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;

import static eu.ehri.project.test.XmlTestHelpers.assertXPath;
import static eu.ehri.project.test.XmlTestHelpers.xPath;


/**
 * Acceptance tests for OAI Repository Explorer Protocol Tester 1.47
 * <p>
 * http://re.cs.uct.ac.za/
 */
public class OaiPmhExporterTest extends XmlExporterTest {

    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    private static final String DEFAULT_LANG_CODE = "eng";

    @Test
    public void testIdentify() throws Exception {
        Document doc = get("verb=" + Verb.Identify);
        assertXPath(doc, "2.0", "//OAI-PMH/Identify/protocolVersion");
    }

    @Test
    public void testIdentifyIllegalParam() throws Exception {
        Document doc = get("verb=" + Verb.Identify + "&test=test");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListMetadataFormats() throws Exception {
        Document doc = get("verb=" + Verb.ListMetadataFormats);
        assertXPath(doc, "oai_dc", "/OAI-PMH/ListMetadataFormats/metadataFormat/metadataPrefix");
        assertXPath(doc, "ead", "/OAI-PMH/ListMetadataFormats/metadataFormat[2]/metadataPrefix");
        assertXPath(doc, "ead3", "/OAI-PMH/ListMetadataFormats/metadataFormat[3]/metadataPrefix");
    }

    @Test
    public void testListSets() throws Exception {
        Document doc = get("verb=" + Verb.ListSets);
        assertXPath(doc, "nl", "/OAI-PMH/ListSets/set/setSpec");
    }

    @Test
    public void testListSetsWithResumptionToken() throws Exception {
        Document page1 = get("verb=" + Verb.ListSets, 2);
        assertXPath(page1, "nl", "/OAI-PMH/ListSets/set/setSpec");
        String rt = (String) xPath(page1, "/OAI-PMH/ListSets/resumptionToken");
        Document page2 = get("verb=" + Verb.ListSets + "&resumptionToken=" + rt);
        assertXPath(page2, "nl:r1", "/OAI-PMH/ListSets/set/setSpec");
        assertXPath(page2, "", "/OAI-PMH/ListSets/resumptionToken");
    }

    @Test
    public void testListIdentifiers() throws Exception {
        Document document = get("verb=" + Verb.ListIdentifiers + "&metadataPrefix=oai_dc");
        assertXPath(document, "c4", "/OAI-PMH/ListIdentifiers/header/identifier");
    }

    @Test
    public void testListIdentifiersWithResumptionToken() throws Exception {
        Document page1 = get("verb=" + Verb.ListIdentifiers + "&metadataPrefix=oai_dc", 2);
        assertXPath(page1, "c4", "/OAI-PMH/ListIdentifiers/header[1]/identifier");
        assertXPath(page1, "nl-000001-1", "/OAI-PMH/ListIdentifiers/header[2]/identifier");
        String rt = (String) xPath(page1, "/OAI-PMH/ListIdentifiers/resumptionToken");
        Document page2 = get("verb=" + Verb.ListIdentifiers + "&resumptionToken=" + rt);
        assertXPath(page2, "nl-r1-m19", "/OAI-PMH/ListIdentifiers/header/identifier");
        assertXPath(page2, "", "/OAI-PMH/ListIdentifiers/resumptionToken");
    }

    @Test
    public void testListIdentifiersWithResumptionTokenAndPrefix() throws Exception {
        Document page1 = get("verb=" + Verb.ListIdentifiers + "&metadataPrefix=oai_dc", 1);
        assertXPath(page1, "c4", "/OAI-PMH/ListIdentifiers/header/identifier");
        String rt = (String) xPath(page1, "/OAI-PMH/ListIdentifiers/resumptionToken");
        Document page2 = get("verb=" + Verb.ListIdentifiers + "&resumptionToken=" + rt + "&metadataPrefix=oai_dc");
        assertError(page2, ErrorCode.badResumptionToken);
    }

    @Test
    public void testListIdentifiersWithRange() throws Exception {
        Document page1 = get("verb=" + Verb.ListIdentifiers + "&metadataPrefix=oai_dc&from=2000-01-01&until=2000-01-01");
        assertError(page1, ErrorCode.noRecordsMatch);
    }

    private void assertError(Document doc, ErrorCode errorCode) throws Exception {
        assertXPath(doc, errorCode.name(), "/OAI-PMH/error/@code");
    }

    @Test
    public void testListIdentifiersWithRangeAndSet() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&set=nl&from=2000-01-01&until=2000-01-01");
        assertXPath(doc, ErrorCode.noRecordsMatch.name(), "/OAI-PMH/error/@code");
    }

    @Test
    public void testListIdentifiersWithIllegalRangeAndSet() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&set=really_wrong_set&from=some_random_date&until=some_random_date");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListIdentifiersWithMismatchedRangeGranularity() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&from=2001-01-01&until=2002-01-01T00:00:00Z");
        assertError(doc, ErrorCode.badArgument);

    }

    @Test
    public void testListIdentifiersWithFromAfterUntil() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&from=2000-01-01&until=1999-01-01");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListIdentifiersWithNoMetadataPrefix() throws Exception {
        Document doc = get("verb=ListIdentifiers");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListIdentifiersWithIllegalMetadataPrefix() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=illegal_mdp");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListIdentifiersWithTwoMetadataPrefixes() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&metadataPrefix=oai_dc");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListIdentifiersWithIllegalResumptionToken() throws Exception {
        Document doc = get("verb=ListIdentifiers&resumptionToken=junktoken");
        assertError(doc, ErrorCode.badResumptionToken);
    }

    @Test
    public void testListIdentifiersWithFromDayGranularity() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&from=2001-01-01");
        assertXPath(doc, "c4", "/OAI-PMH/ListIdentifiers/header/identifier");
    }

    @Test
    public void testListIdentifiersWithFromSecondGranularity() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&from=2001-01-01");
        assertXPath(doc, "c4", "/OAI-PMH/ListIdentifiers/header/identifier");
    }

    @Test
    public void testListIdentifiersWithFromInvalidGranularity() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&from=2001");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListMetadataFormatsWithIdentifier() throws Exception {
        Document doc = get("verb=ListMetadataFormats&identifier=c4");
        assertXPath(doc, "oai_dc", "/OAI-PMH/ListMetadataFormats/metadataFormat/metadataPrefix");
    }

    @Test
    public void testListMetadataFormatsWithIllegalIdentifier() throws Exception {
        Document doc = get("verb=ListMetadataFormats&identifier=really_wrong_id");
        assertError(doc, ErrorCode.idDoesNotExist);
    }

    @Test
    public void testGetRecord() throws Exception {
        Document doc = get("verb=GetRecord&identifier=c4&metadataPrefix=oai_dc");
        assertXPath(doc, "c4", "//OAI-PMH/GetRecord/record/header/identifier");
    }

    @Test
    public void testGetRecordWithNoMetadataPrefix() throws Exception {
        Document doc = get("verb=GetRecord&identifier=c4");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testGetRecordWithIllegalMetadataPrefix() throws Exception {
        Document doc = get("verb=GetRecord&identifier=c4&metadataPrefix=really_wrong_mdp");
        assertXPath(doc, ErrorCode.badArgument.name(), "//OAI-PMH/error/@code");
    }

    @Test
    public void testGetRecordWithError() throws Exception {
        Document doc = get("verb=" + Verb.GetRecord + "&metadataPrefix=oai_dc&identifier=c1");
        assertError(doc, ErrorCode.idDoesNotExist);
    }

    @Test
    public void testListRecordsWithRange() throws Exception {
        Document doc = get("verb=ListRecords&metadataPrefix=oai_dc&from=2000-01-01&until=2000-01-01");
        assertError(doc, ErrorCode.noRecordsMatch);
    }

    @Test
    public void testListRecordsWithIllegalRangeAndSet() throws Exception {
        Document doc = get("verb=ListRecords&metadataPrefix=oai_dc&set=really_wrong_set&from=some_random_date&until=some_random_date");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListRecordsWithNoMetadataPrefix() throws Exception {
        Document doc = get("verb=ListRecords");
        assertError(doc, ErrorCode.badArgument);
    }

    @Test
    public void testListRecords() throws Exception {
        Document doc = get("verb=ListRecords&metadataPrefix=oai_dc");
        assertXPath(doc, "c4", "//OAI-PMH/ListRecords/record/header/identifier");
    }

    @Test
    public void testListRecordsUntilBeforeEarliestDate() throws Exception {
        String earliestDate = ZonedDateTime.parse(
                anonApi().actionManager().getEventRoot().getTimestamp())
                .minusDays(1)
                .format(OaiPmhExporter.DATE_PATTERN);
        Document doc = get("verb=ListRecords&metadataPrefix=oai_dc&until=" + earliestDate);
        assertError(doc, ErrorCode.noRecordsMatch);
    }

    @Test
    public void testListRecordsWithBadResumptionToken() throws Exception {
        Document doc = get("verb=ListRecords&resumptionToken=junktoken");
        assertError(doc, ErrorCode.badResumptionToken);
    }

    @Test
    public void testListIdentifiersWithSet() throws Exception {
        Document doc = get("verb=ListIdentifiers&metadataPrefix=oai_dc&set=nl");
        assertXPath(doc, "c4", "//OAI-PMH/ListIdentifiers/header/identifier");
    }

    @Test
    public void testGetRecordSetSpec() throws Exception {
        Document doc = get("verb=GetRecord&identifier=c4&metadataPrefix=oai_dc");
        assertXPath(doc, "nl", "//OAI-PMH/GetRecord/record/header/setSpec[1]");
        assertXPath(doc, "nl:r1", "//OAI-PMH/GetRecord/record/header/setSpec[2]");
    }

    @Test
    public void testIllegalVerb() throws Exception {
        Document doc = get("verb=IllegalVerb");
        assertError(doc, ErrorCode.badVerb);
    }

    private Document get(String params) throws Exception {
        return get(params, 10);
    }

    private Document get(String params, int limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final IndentingXMLStreamWriter sw = new IndentingXMLStreamWriter(
                xmlOutputFactory.createXMLStreamWriter(new BufferedOutputStream(out)))) {
            Api api = anonApi();
            OaiPmhExporter oaiPmh = new OaiPmhExporter(OaiPmhData.create(api, true),
                    OaiPmhRenderer.defaultRenderer(api, DEFAULT_LANG_CODE), ConfigFactory.load());
            try {
                OaiPmhState state = OaiPmhState.parse(params, limit);
                oaiPmh.performVerb(sw, state);
            } catch (OaiPmhError e) {
                oaiPmh.error(sw, e.getCode(), e.getMessage(), null);
            }
            sw.flush();
        }
        String xml = out.toString();
        //System.out.println(xml);
        isValid(xml);
        return XmlTestHelpers.parseDocument(xml);
    }

    private void isValid(String xml) throws IOException, SAXException {
        // FIXME: Why won't this validate with the proper schema?
        XmlTestHelpers.validatesSchema(xml, "OAI-PMH-lax.xsd");
    }
}