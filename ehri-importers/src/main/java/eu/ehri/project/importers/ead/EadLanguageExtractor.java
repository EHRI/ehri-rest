package eu.ehri.project.importers.ead;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EadLanguageExtractor {

    private static final Logger logger = LoggerFactory
            .getLogger(EadLanguageExtractor.class);

    @SuppressWarnings("serial")
    public final Map<String, String> iso639BibTermLookup = new HashMap<String, String>() {
        {
            put("heb", "he");
            put("ger", "de");
            put("rum", "ro");
            put("yid", "yi");
            put("sqi", "sq");
            put("dut", "nl");
            put("slo", "sk");
            put("fre", "fr");
            put("cze", "cs");
        }
    };

    public Map<String, Locale> threeLetterLanguageMap = new HashMap<String, Locale>();
    public Map<String, Locale> nameLanguageMap = new HashMap<String, Locale>();

    protected List<String> languagesOfDescription;
    protected List<String> languagesOfMaterial;

    private final XPath xpath;
    private final Node topLevelEad;

    public EadLanguageExtractor(XPath xpath, Node data) {
        this.topLevelEad = data;
        this.xpath = xpath;

        // Initialise the locale map
        String[] languages = Locale.getISOLanguages();
        for (String language : languages) {
            Locale locale = new Locale(language);
            threeLetterLanguageMap.put(locale.getISO3Language().toLowerCase(),
                    locale);
            nameLanguageMap.put(locale.getDisplayLanguage().toLowerCase(),
                    locale);
        }

        // Hack: Add terminology codes for a few languages

        // Sniff the top-level language codes.
        // TODO: Make this not suck...
        languagesOfDescription = getTopLevelLanguagesOfDescription();
        languagesOfMaterial = getTopLevelLanguagesOfMaterial();

    }

    public String getLanguageOfDescription(Node data) {
        List<String> codes;
        try {
            codes = getLanguageCodes(data, "langusage/language");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // TODO: Sanity check the results and handle if there's
        // more than one language?
        // Default to current locale?
        if (codes.isEmpty())
            return Locale.getDefault().getLanguage();
        return codes.get(0);
    }

    public List<String> getLanguagesOfMaterial(Node data) {
        try {
            return getLanguageCodes(data, "did/langmaterial/language");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<String> getTopLevelLanguagesOfMaterial() {
        // Get the nearest archdesc node and search it for a langmaterial
        // attribute.
        List<String> langs = new LinkedList<String>();
        Node node = null;
        try {
            node = (Node) xpath.compile("archdesc").evaluate(topLevelEad,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (node != null) {
            Node attr = node.getAttributes().getNamedItem("langmaterial");
            if (attr != null) {
                logger.debug("Got langmaterial node: " + attr.getNodeValue());
                Locale loc = threeLetterLanguageMap.get(attr.getNodeValue()
                        .toLowerCase());

                if (loc != null) {
                    logger.debug("Adding language of material: "
                            + loc.getLanguage());
                    langs.add(loc.getLanguage());
                }
            }
        }
        logger.debug("Got top level language of material: " + langs);
        return langs;
    }

    private List<String> getTopLevelLanguagesOfDescription() {
        List<String> langs = new LinkedList<String>();

        // Look in eadheader/profiledesc/langusage/language
        NodeList profDescLang = null;
        try {
            profDescLang = (NodeList) xpath.compile(
                    "eadheader/profiledesc/langusage/language").evaluate(
                    topLevelEad, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < profDescLang.getLength(); i++) {
            String langCode = getLanguageCodeFromLanguageNode(profDescLang
                    .item(i));
            ;
            if (langCode != null) {
                langs.add(langCode);
                logger.debug("Got top level lang: " + langCode);
            }
        }
        logger.debug("Got top level language of description: " + langs);
        return langs;
    }

    /**
     * Try and match language material in the given path to an actual locale.
     * 
     * @param data
     * @param path
     * @return
     * @throws XPathExpressionException
     */
    private List<String> getLanguageCodes(Node data, String path)
            throws XPathExpressionException {
        // Look for the 3-letter ISO639-2b language code. If that's not there,
        // then try to match the language text.
        // FIXME: Uncertainties about variable casing that might
        // cause matches to fail.
        List<String> locList = new LinkedList<String>();
        NodeList langs = (NodeList) xpath.compile(path).evaluate(data,
                XPathConstants.NODESET);
        for (int i = 0; i < langs.getLength(); i++) {

            String langCode = getLanguageCodeFromLanguageNode(langs.item(i));
            if (langCode != null) {
                locList.add(langCode);
            }
        }

        // Fall back on whatever text is given, if any...
        if (locList.isEmpty()) {
            String langText = (String) xpath.compile(path).evaluate(data,
                    XPathConstants.STRING);
            if (!langText.trim().isEmpty()) {
                locList.add(langText);
            }
        }

        return locList;
    }

    private String getLanguageCodeFromLanguageNode(Node item) {
        Node codeNode = item.getAttributes().getNamedItem("langcode");
        String text = item.getTextContent().trim();
        String code = null;
        if (codeNode != null) {
            Locale loc = threeLetterLanguageMap.get(codeNode.getNodeValue()
                    .toLowerCase());
            if (loc != null) {
                logger.debug("Got language from node: " + loc.getLanguage()
                        + " " + loc.getDisplayLanguage());
                code = loc.getLanguage();
            } else {
                code = iso639BibTermLookup.get(codeNode.getNodeValue()
                        .toLowerCase());
            }
        } else if (!text.isEmpty()) {
            // FIXME: Do we need to
            Locale loc = nameLanguageMap.get(text.toLowerCase());
            if (loc != null) {
                code = loc.getLanguage();
            }
        }
        logger.debug("Language for code: " + item.getTextContent() + " " + code);
        return code;
    }

    public Collection<String> getGlobalLanguagesOfDescription() {
        return languagesOfDescription;
    }

    public Collection<String> getGlobalLanguagesOfMaterial() {
        return languagesOfMaterial;
    }
}