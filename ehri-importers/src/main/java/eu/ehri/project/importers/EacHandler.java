package eu.ehri.project.importers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import org.xml.sax.Attributes;

/**
 * makes use of eac.properties with format: part/of/path/=attribute
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class EacHandler extends EaHandler {

    private final ImmutableMap<String, Class<? extends Frame>> possibleSubnodes
            = ImmutableMap.<String, Class<? extends Frame>>builder()
                .put("maintenanceEvent", MaintenanceEvent.class)
                .put("relation", Annotation.class)
                .put("book", Annotation.class)
                .put("bookentry", Annotation.class)
                .put("accessPoint", Annotation.class)
                .put("name", UnknownProperty.class).build();

    private static final Logger logger = LoggerFactory.getLogger(EacHandler.class);

    public EacHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("eac.properties"));
    }
    public EacHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super(importer, properties);
    }
    @Override
    protected boolean needToCreateSubNode(String key) {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode(getImportantPath(currentPath))) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();

        //an EAC file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                }

                //TODO: name can have only 1 value, others are otherFormsOfName
                if (currentGraphPath.peek().containsKey(Ontology.NAME_KEY)) {
                    String name = chooseName(currentGraphPath.peek().get(Ontology.NAME_KEY));
                    overwritePropertyInCurrentGraph(Ontology.NAME_KEY, name);
                }
                if (!currentGraphPath.peek().containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
                    logger.debug("no " + Ontology.LANGUAGE_OF_DESCRIPTION + " found");
                    putPropertyInCurrentGraph(Ontology.LANGUAGE_OF_DESCRIPTION, "en");
                }

                importer.importItem(currentGraphPath.pop(), Lists.<String>newArrayList());

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    @Override
    protected List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        schemas.add("eac.xsd");
        return schemas;
    }
}
