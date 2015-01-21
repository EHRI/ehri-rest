package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Import Map based representations of EAD or EAC for a given repository into the database.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 *
 */
public abstract class EaImporter extends MapImporter {

    private static final Logger logger = LoggerFactory.getLogger(EaImporter.class);
    protected static final String LINK_TARGET = "target";

    public static final String RESOLVED_LINK_DESC = "Link provided by data provider.";

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EaImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    /**
     * Extract properties from the itemData Map that belong to a generic unit and
     * returns them as a new Map. Calls extractDocumentaryUnit.
     * 
     * @param itemData a Map representation of a unit
     * @return a new Map containing those properties that are specific to a unit
     * @throws ValidationError when extractDocumentaryUnit throws it
     */
    protected Map<String, Object> extractUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = extractDocumentaryUnit(itemData);
        unit.put("typeOfEntity", itemData.get("typeOfEntity"));
        return unit;
    }

    /**
     * Extract DocumentaryUnit properties from the itemData and return them as a new Map.
     * This implementation only extracts the objectIdentifier.
     * 
     * This implementation does not throw ValidationErrors.
     * 
     * @param itemData a Map containing raw properties of a DocumentaryUnit
     * @return a new Map containing the objectIdentifier property
     * @throws ValidationError never
     */
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
        return unit;
    }
     
    /**
     * Extract properties from the itemData Map that are marked as unknown, and return them in a new Map.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all keys from itemData that start with SaxXmlHandler.UNKNOWN
     * @throws ValidationError never
     */
    protected Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = new HashMap<String, Object>();
        for (Entry<String, Object> key : itemData.entrySet()) {
            if (key.getKey().startsWith(SaxXmlHandler.UNKNOWN)) {
                unknowns.put(key.getKey().substring(SaxXmlHandler.UNKNOWN.length()), key.getValue());
            }
        }
        return unknowns;
    }
    
    /**
     * Extract node representations for related nodes based on the 'relation' property in the supplied data Map.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return an Iterable of new Maps representing related nodes and their types
     */
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> itemData) {
        final String REL = "relation";
        List<Map<String, Object>> listOfRelations = new ArrayList<Map<String, Object>>();
        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals(REL)) {
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) itemProperty.getValue()) {
                    Map<String, Object> relationNode = new HashMap<String, Object>();
                    for (Entry<String, Object> relationProperty : origRelation.entrySet()) {
                        if (relationProperty.getKey().equals(REL + "/type")) {
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(REL + "/url")) {
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(REL + "/" + Ontology.NAME_KEY)) {
                            //try to find the original identifier
                            relationNode.put(Ontology.NAME_KEY, relationProperty.getValue());
                        } else if (relationProperty.getKey().equals(REL + "/notes")) {
                            relationNode.put(Ontology.LINK_HAS_DESCRIPTION, relationProperty.getValue());
                        } else {
                            relationNode.put(relationProperty.getKey(), relationProperty.getValue());
                        }
                    }
                    // Set a default relationship type if no type was found in the relationship
                    if (!relationNode.containsKey(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)) {
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccess");
                    }
                    listOfRelations.add(relationNode);
                }
            }
        }
        return listOfRelations;
    }

    /**
     * Extract a Map containing the properties of a documentary unit's description.
     * Excludes unknown properties, object identifier(s), maintenance events, relations,
     * addresses and *Access relations.
     * 
     * @param itemData a Map containing raw properties of a unit 
     * @param entity an EntityClass to get the multi-valuedness of properties for
     * @return a Map representation of a DocumentDescription
     */
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = new HashMap<String, Object>();

        description.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemProperty.getValue());
            } else if (itemProperty.getKey().equals("conditionsOfAccess")) {
                description.put(itemProperty.getKey(), changeForbiddenMultivaluedProperties(itemProperty.getKey(), itemProperty.getValue(), entity));
            } else if ( !itemProperty.getKey().startsWith(SaxXmlHandler.UNKNOWN) 
                    && ! itemProperty.getKey().equals("objectIdentifier") 
                    && ! itemProperty.getKey().equals(Ontology.IDENTIFIER_KEY)
                    && ! itemProperty.getKey().equals(Ontology.OTHER_IDENTIFIERS)
                    && ! itemProperty.getKey().startsWith("maintenanceEvent") 
                    && ! itemProperty.getKey().startsWith("relation")
                    && ! itemProperty.getKey().startsWith("IGNORE")
                    && ! itemProperty.getKey().startsWith("address/")
                    && ! itemProperty.getKey().endsWith("Access")
                    && ! itemProperty.getKey().endsWith("AccessPoint")) {
               description.put(itemProperty.getKey(), changeForbiddenMultivaluedProperties(itemProperty.getKey(), itemProperty.getValue(), entity));
            }
        }
//        assert(description.containsKey(IdentifiableEntity.IDENTIFIER_KEY));
        return description;
    }
    
    /**
     * Extract an address node representation from the itemData.
     * 
     * @param itemData a Map containing raw properties of a unit
     * @return returns a Map with all address/ keys (may be empty)
     */
    protected Map<String, Object> extractAddress(Map<String, Object> itemData)  {
        Map<String, Object> address = new HashMap<String, Object>();
        for (Entry<String, Object> itemProperty : itemData.entrySet()) {
            if (itemProperty.getKey().startsWith("address/")) {
                address.put(itemProperty.getKey().substring(8), itemProperty.getValue());
            }
        }
        return address;
    }
}
