package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Handler of EADs exported from ICA-AtoM.
 * Only titles have to be handled in a special way.
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class IcaAtomEadHandler extends EadHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(IcaAtomEadHandler.class);
    private final List<DocumentaryUnit>[] children = new ArrayList[12];
    
    
    @SuppressWarnings("unchecked")
    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("icaatom.properties"));
        children[depth] = new ArrayList<DocumentaryUnit>();
    }

    
    @Override
    protected void extractTitle(Map<String, Object> currentGraph) {
    	if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
            //finding some name for this unit:
            if (currentGraph.containsKey("title")) {
                currentGraph.put(Ontology.NAME_KEY, currentGraph.get("title"));
            } else {
                logger.error("IcaAtom node without name field: ");
                currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title");
            }
        }
    }

}
