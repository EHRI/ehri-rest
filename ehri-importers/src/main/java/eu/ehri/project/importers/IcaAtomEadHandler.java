package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler of EADs exported from ICA-AtoM.
 * Only titles have to be handled in a special way.
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class IcaAtomEadHandler extends EadHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(IcaAtomEadHandler.class);
    private final List<DocumentaryUnit>[] children = new ArrayList[12];

    /**
     * Set a custom resolver so EAD DTDs are never looked up online.
     * @param publicId
     * @param systemId
     * @return returns essentially an empty dtd file
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
            throws org.xml.sax.SAXException, java.io.IOException {
        // This is the equivalent of returning a null dtd.
        return new org.xml.sax.InputSource(new java.io.StringReader(""));
    }
    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super(importer, properties);
        children[depth] = new ArrayList<DocumentaryUnit>();
    }
    @SuppressWarnings("unchecked")
    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties("icaatom.properties"));
    }

    @Override
    protected void extractTitle(Map<String, Object> currentGraph) {
    	if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
            //finding some name for this unit:
            if (currentGraph.containsKey("title")) {
                currentGraph.put(Ontology.NAME_KEY, currentGraph.get("title"));
            } else {
                logger.warn("DocumentaryUnit node without name field: ");
                currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title");
            }
        }

    }

}
