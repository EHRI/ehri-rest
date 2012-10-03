package eu.ehri.project.importers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ead.EadLanguageExtractor;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Import EAD for a given repository into the database. Due to the laxness of
 * the EAD standard this is a fairly complex procedure. An EAD a single entity
 * at the highest level of description or multiple top-level entities, with or
 * without a heirarchical structure describing their child items. This means
 * that we need to recursively descend through the archdesc and c01-12 levels.
 * 
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 * 
 * @author michaelb
 * 
 */
public class EadImporter extends AbstractMultiItemRecursiveImporter<Node> {

    private static final Logger logger = LoggerFactory
            .getLogger(EadImporter.class);

    private final XPath xpath;
    private EadLanguageExtractor langHelper;

    // An integer that represents how far down the
    // EAD heirarchy tree the current document is.
    public final String DEPTH_ATTR = "depthOfDescription";

    // A (possibly arbitrary) string denoting what the
    // describing body saw fit to name a documentary unit's
    // level of description.
    public final String LEVEL_ATTR = "levelOfDescription";

    // Global EAD Attributes (stored in eadheader).
    @SuppressWarnings("serial")
    private final Map<String,String> eadGlobalAttributeMap = new HashMap<String, String>() {
        {
            put("publisher", "eadheader/filedesc/publicationstmt/publisher");
            put("publicationDate", "eadheader/filedesc/date");
            put("creationDate", "profiledesc/creation/date");
            put("rules", "profiledesc/descrules");
        }
    };
    
    // List of XPaths against the final attribute names. This should probably be
    // moved
    // to external configuration...
    @SuppressWarnings("serial")
    private final Map<String, String> eadAttributeMap = new HashMap<String, String>() {
        {
            put("accruals", "accurals/p");
            put("acquisition", "acqinfo/p");
            put("appraisal", "appraisal/p");
            put("archivalHistory", "custodhist/p");
            put("conditionsOfAccess", "accessrestrict/p");
            put("conditionsOfReproduction", "userestrict/p");
            put("extentAndMedium", "did/physdesc/extent"); // Should be more
                                                           // nuanced!
            put(AccessibleEntity.IDENTIFIER_KEY, "did/unitid");
            put("locationOfCopies", "altformavail/p");
            put("locationOfOriginals", "originalsloc/p");
            put("title", "did/unittitle");
            put("physicalCharacteristics", "phystech/p");
            put("scopeAndContent", "scopecontent/p");
            put("systemOfArrangement", "arrangement/p");
        }
    };

    @SuppressWarnings("serial")
    private final Map<String, String> eadControlaccessMap = new HashMap<String, String>() {
        {
            put("subjects", "controlaccess/subjects");
            put("creators", "did/origination/persname");
            put("creators", "did/origination/corpname");
            put("nameAccess", "controlaccess/persname");
            put("corporateBodies", "controlaccess/corpname");
            put("places", "controlaccess/geogname");
            put("unitDates", "did/unitdate");
        }
    };

    // Pattern for EAD nodes that represent a child item
    private Pattern childItemPattern = Pattern.compile("^c(?:\\d+)$");

    // Various date patterns
    private Pattern[] datePatterns = {
            // Yad Vashem, ICA-Atom style: 1924-1-1 - 1947-12-31
            Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2})\\s?-\\s?(\\d{4}-\\d{1,2}-\\d{1,2})$"),
            Pattern.compile("^(\\d{4})\\s?-\\s?(\\d{4})$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4})-\\[(\\d{4})\\]$"),
            Pattern.compile("^(\\d{4}s)-\\[(\\d{4}s)\\]$"),
            Pattern.compile("^\\[(\\d{4})\\]$"), Pattern.compile("^(\\d{4})$"),
            Pattern.compile("^(\\d{2})th century$") };

    /**
     * Construct an EadImporter object.
     * 
     * @param framedGraph
     * @param repository
     * @param topLevelEad
     */
    public EadImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            Node topLevelEad, ImportLog log) {
        super(framedGraph, repository, log, topLevelEad);

        xpath = XPathFactory.newInstance().newXPath();
        langHelper = new EadLanguageExtractor(xpath, topLevelEad);
    }

    /**
     * Extract a list of entity bundles for DatePeriods from the data,
     * attempting to parse the unitdate attribute.
     * 
     * @param data
     */
    @Override
    public List<Map<String, Object>> extractDates(Node data) {
        List<Map<String, Object>> dates = new LinkedList<Map<String, Object>>();
        try {
            for (String date : extractTextList(data, "did/unitdate")) {
                Map<String, Object> dpb = extractDate(date);
                if (dpb != null)
                    dates.add(dpb);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ValidationError e) {
            System.out.println("ERROR WITH DATES");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return dates;
    }

    public List<Node> extractChildItems(Node data) {
        List<Node> children = new LinkedList<Node>();
        logger.debug("Extracting child data from: " + data);
        for (int i = 0; i < data.getChildNodes().getLength(); i++) {
            Node n = data.getChildNodes().item(i);
            if (childItemPattern.matcher(n.getNodeName()).matches()) {
                children.add(n);
            }
        }
        return children;
    }

    @Override
    protected Map<String, Object> extractDocumentaryUnit(Node data, int depth)
            throws ValidationError {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        try {
            dataMap.put(AccessibleEntity.IDENTIFIER_KEY, xpath.compile("did/unitid/text()")
                    .evaluate(data, XPathConstants.STRING));
            dataMap.put(
                    DocumentaryUnit.NAME,
                    xpath.compile("did/unittitle/text()").evaluate(data,
                            XPathConstants.STRING));

            logger.info("Importing item: " + dataMap.get("identifier")
                    + " at depth: " + depth);

            // Add persname, origination etc
            for (Entry<String, String> entry : eadControlaccessMap.entrySet()) {
                List<String> vals = extractTextList(data, entry.getValue());
                dataMap.put(entry.getKey(),
                        vals.toArray(new String[vals.size()]));
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return dataMap;
    }

    /**
     * Extract the document descriptions from the node data. For EAD, there will
     * only be one description.
     * 
     * @param data
     * @param depth
     * @return
     */
    @Override
    public List<Map<String, Object>> extractDocumentDescriptions(Node data,
            int depth) throws ValidationError {
        List<Map<String, Object>> descs = new LinkedList<Map<String, Object>>();

        Map<String, Object> dataMap = new HashMap<String, Object>();

        // For EAD (and most other types) there will only be one description
        // per logical object we create.
        
        // Extract global attributes from the document context
        for (Entry<String, String> entry : eadGlobalAttributeMap.entrySet()) {
            dataMap.put(entry.getKey(), getElementText(documentContext, entry.getValue()));
        }
        
        // And body attrubutes from the archdesc/c01/c02/c node...
        for (Entry<String, String> entry : eadAttributeMap.entrySet()) {
            dataMap.put(entry.getKey(), getElementText(data, entry.getValue()));
        }

        // Set level of description and actual level of nesting
        dataMap.put(LEVEL_ATTR, getLevelOfDescription(data));
        dataMap.put(DEPTH_ATTR, depth);

        // Get language of description... a single string code.
        dataMap.put("languageCode", langHelper.getLanguageOfDescription(data));

        // Set language of materials
        // FIXME: This is pretty horrible.
        List<String> materialCodes = langHelper.getLanguagesOfMaterial(data);
        materialCodes.addAll(langHelper.getGlobalLanguagesOfMaterial());
        dataMap.put("languagesOfMaterial", getUniqueArray(materialCodes));

        descs.add(dataMap);

        return descs;
    }

    /**
     * Extract items from a node which may contain multiple top-level items or
     * just a single item. For example:
     * 
     * &lt;archdesc level=&quot;fonds&quot;&gt; &lt;did&gt;
     * &lt;unittitle&gt;Test Item&lt;/unittitle&gt; &lt/did&gt; ...
     * &lt/archdesc&gt;
     * 
     * or
     * 
     * &lt;archdesc level=&quot;fonds&quot;&gt; &lt;dsc&gt; &lt;c01&gt;
     * 
     * &lt;did&gt; &lt;unittitle&gt;Test Item 1&lt;/unittitle&gt; &lt/did&gt;
     * ... &lt;c01&gt; &lt;c01&gt;
     * 
     * &lt;did&gt; &lt;unittitle&gt;Test Item 2&lt;/unittitle&gt; &lt/did&gt;
     * ... &lt;c01&gt; &lt;dsc&gt; &lt/archdesc&gt;
     * 
     */
    protected List<Node> getEntryPoints() throws ValidationError,
            InvalidInputFormatError {
        List<Node> entryPoints = new LinkedList<Node>();
        try {
            NodeList nodes = (NodeList) xpath.compile("archdesc").evaluate(
                    documentContext, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node archDescNode = nodes.item(i);

                try {
                    Node titleNode = (Node) xpath.compile("did/unittitle")
                            .evaluate(archDescNode, XPathConstants.NODE);
                    // If we have a unitid at archdesc level, import that
                    if (titleNode != null
                            && !titleNode.getTextContent().trim().isEmpty()) {
                        logger.info("Extracting single item from archdesc... "
                                + titleNode.getTextContent());
                        entryPoints.add(archDescNode);
                    } else {
                        // Otherwise, inspect the children of the archdesc/dsc
                        logger.info("Extracting multiple items from archdesc/dsc...");
                        NodeList dsc = (NodeList) xpath.compile("dsc")
                                .evaluate(archDescNode, XPathConstants.NODESET);
                        for (int j = 0; j < dsc.getLength(); j++) {
                            for (Node d : extractChildItems(dsc.item(j))) {
                                entryPoints.add(d);
                            }
                        }
                    }
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (entryPoints.size() == 0)
            throw new InvalidInputFormatError(
                    "No archdesc/did or archdesc/dsc/c* elements found");

        return entryPoints;
    }

    // Helpers

    /**
     * Make a list unique.
     * 
     * @param list
     * @return
     */
    private String[] getUniqueArray(List<String> list) {
        HashSet<String> hs = new HashSet<String>();
        hs.addAll(list);
        return hs.toArray(new String[list.size()]);
    }

    /**
     * Extract a list of strings from the specified element set.
     * 
     * @param data
     * @param path
     * @return
     * @throws XPathExpressionException
     */
    private List<String> extractTextList(Node data, String path)
            throws XPathExpressionException {
        List<String> names = new ArrayList<String>();

        NodeList nodes = (NodeList) xpath.compile(path).evaluate(data,
                XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getTextContent());
        }
        return names;
    }

    /**
     * Extract the level of description.
     * 
     * @param data
     * @return
     */
    private String getLevelOfDescription(Node data) {
        // TODO: This function probably does not explore all
        Node attr = data.getAttributes().getNamedItem("level");
        if (attr != null)
            return attr.getNodeValue();
        return "";
    }

    /**
     * Attempt to extract some date periods. This does not currently put the
     * dates into ISO form.
     * 
     * @param date
     * @return
     * @throws ValidationError
     */
    private Map<String, Object> extractDate(String date) throws ValidationError {
        Map<String, Object> data = new HashMap<String, Object>();
        boolean ok = false;
        for (Pattern re : datePatterns) {
            Matcher matcher = re.matcher(date);
            if (matcher.matches()) {
                data.put(DatePeriod.START_DATE, normaliseDate(matcher.group(1)));
                data.put(DatePeriod.END_DATE, normaliseDate(matcher.group(matcher
                        .groupCount() > 1 ? 2 : 1)));
                ok = true;
                break;
            }
        }

        return ok ? data : null;
    }

    /**
     * Fetch the element text from (possibly) multiple nodes, and join them with
     * a double break.
     * 
     * @param data
     * @param path
     * @return
     * @throws XPathExpressionException
     */
    private String getElementText(Node data, String path) {
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.compile(path).evaluate(data,
                    XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        String out = "";
        for (int i = 0; i < nodes.getLength(); i++) {
            if (out.isEmpty()) {
                out += nodes.item(i).getTextContent();
            } else {
                out += "\n\n" + nodes.item(i).getTextContent();
            }
        }
        return out;
    }

    private String normaliseDate(String date) {
        DateTimeFormatter fmt = ISODateTimeFormat.date();
        return fmt.print(DateTime.parse(date));
    }
}
