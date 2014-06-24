package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.persistence.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAD describing a Virtual Collection.
 * some rules governing virtual collections:
 *	
 *	the archdesc should describe the purpose of this vc. it can not in itself refer to a DU.
 *	
 *	every c level is either 1) a virtual level (=VirtualLevel), or 2) it points to an existing DocumentaryUnit (=VirtualReferrer) (and consequently to the entire subtree beneath it)
 *	1) there is no repository-tag with a ehri-label
 *	
 *	2) there is exactly one repository-tag with an ehri-label
 *	<repository label="ehri_repository_vc">il-002777</repository>
 *	(this will not be shown in the portal)
 *	and exactly one unitid with a ehri-main-identifier label, that is identical to the existing unitid within the graph for this repository
 *	
 *	all other tags will be ignored, since the DocumentsDescription of the referred DocumentaryUnit will be shown.
 *	there should not be any c-levels beneath such a c-level
 *	
 *
 * @author lindar
 *
 */
public class VirtualEadImporter extends EaImporter {
    protected static final String REPOID="vcRepository";
    protected static final String UNITID="objectIdentifier";
    

    private static final Logger logger = LoggerFactory.getLogger(VirtualEadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private EntityClass unitEntity = EntityClass.VIRTUAL_UNIT;

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public VirtualEadImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     * this will import the structure as VirtualUnits, which either have a DocDescription (VirtualLevel, like series)
     * or they point to an existing DocDesc from an existing DocumentaryUnit (VirtualReferrer).
     *
     * @param itemData The data map
     * @param idPath The identifiers of parent documents,
     *               not including those of the overall permission scope
     * @throws ValidationError when the itemData does not contain an identifier for the unit or...
     */
    @Override
    public AbstractUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        boolean isVirtualLevel = isVirtualLevel(itemData);
        Bundle unit = new Bundle(unitEntity, extractVirtualUnit(itemData));
        
        if (isVirtualLevel) { //VirtualLevel
            // Check for missing identifier, throw an exception when there is no ID.
            if (unit.getDataValue(Ontology.IDENTIFIER_KEY) == null) {
                throw new ValidationError(unit, Ontology.IDENTIFIER_KEY,
                        "Missing identifier " + Ontology.IDENTIFIER_KEY);
            }
            logger.debug("Imported item: " + itemData.get("name"));
            Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENT_DESCRIPTION));
            // Add dates and descriptions to the bundle since they're @Dependent
            // relations.
            for (Map<String, Object> dpb : extractDates(itemData)) {
                descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
            }
            for (Map<String, Object> rel : extractRelations(itemData)) {//, (String) unit.getErrors().get(IdentifiableEntity.IDENTIFIER_KEY)
                logger.debug("relation found: " + rel.get(Ontology.NAME_KEY));
                descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
            }
            Map<String, Object> unknowns = extractUnknownProperties(itemData);
            if (!unknowns.isEmpty()) {
                logger.debug("Unknown Properties found");
                descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
            }
            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }

        Mutation<VirtualUnit> mutation =
                persister.createOrUpdate(unit, VirtualUnit.class);
        VirtualUnit frame = mutation.getNode();

        //add the referencedTo DocumentDescriptions
        if (!isVirtualLevel) {//VirtualReferrer
            try {
                //find the DocumentaryUnit using the repository_id/unit_id combo
                DocumentaryUnit d = findReferredToDocumentaryUnit(itemData);
                for (DocumentDescription desc : d.getDocumentDescriptions()) {
                    frame.addReferencedDescription(desc);
                }
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, ex.getKey(), ex.getMessage());
            }
        }

        // Set the repository/item relationship
        //TODO: figure out another way to determine we're at the root, so we can get rid of the depth param
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(permissionScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                UserProfile responsibleUser = framedGraph.frame(permissionScope.asVertex(), UserProfile.class);
                frame.setAuthor(responsibleUser);
                frame.setPermissionScope(responsibleUser);
            } else if (scopeType.equals(unitEntity)) {
                VirtualUnit parent = framedGraph.frame(permissionScope.asVertex(), VirtualUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else if(unitEntity.equals(EntityClass.VIRTUAL_UNIT)) {
              // no scope needed for top VirtualUnit
            } else {
                logger.error("Unknown scope type for virtual unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);
//        if (mutation.created()) {
//            solveUndeterminedRelationships(frame, descBundle);
//        }
        return frame;


    }

    

    @SuppressWarnings("unchecked")
   @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        final String REL = "AccessPoint";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : data.keySet()) {
            if (key.endsWith(REL)) {
                logger.debug(key + " found in data");
                //type, targetUrl, targetName, notes
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> relationNode = new HashMap<String, Object>();
                    for (String eventkey : origRelation.keySet()) {
                        logger.debug(eventkey);
                        if (eventkey.endsWith(REL)) {
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, eventkey);
                            relationNode.put(Ontology.NAME_KEY, origRelation.get(eventkey));
                        } else {
                            relationNode.put(eventkey, origRelation.get(eventkey));
                        }
                    }
                    if (!relationNode.containsKey(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)) {
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccessPoint");
                    }
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    /**
     * Creates a Map containing properties of a Virtual Unit.
     * 
     * These properties are the unit's identifiers.
     * @param itemData Map of all extracted information
     * @return a Map representing a Documentary Unit node
     * @throws ValidationError
     */
    protected Map<String, Object> extractVirtualUnit(Map<String, Object> itemData) throws ValidationError {
        
        Map<String, Object> unit = new HashMap<String, Object>();
        if (itemData.get(OBJECT_ID) != null) {
            unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_ID));
        }
        if (itemData.get(Ontology.OTHER_IDENTIFIERS) != null) {
        	logger.debug("otherIdentifiers is not null");
            unit.put(Ontology.OTHER_IDENTIFIERS, itemData.get(Ontology.OTHER_IDENTIFIERS));
        }
        return unit;
    }

    @Override
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }
    /**
     * Creates a Map containing properties of a Documentary Unit description.
     * These properties are the unit description's properties: all except the doc unit identifiers and unknown properties.
     * @param itemData Map of all extracted information
     * @param depth depth of node in the tree
     * @return a Map representing a Documentary Unit Description node
     * @throws ValidationError
     */
    protected Map<String, Object> extractDocumentDescription(Map<String, Object> itemData, int depth) throws ValidationError {

        Map<String, Object> unit = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (!(key.equals(OBJECT_ID) 
            	|| key.equals(Ontology.OTHER_IDENTIFIERS) 
            	|| key.startsWith(SaxXmlHandler.UNKNOWN))) {
                unit.put(key, itemData.get(key));
            }
        }
        return unit;
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void importAsVirtualCollection(){
      unitEntity = EntityClass.VIRTUAL_UNIT;
    }

    /**
     * if the itemData contains a known repository_id/unit_id pair, this will return false, true otherwise
     * @param itemData
     * @return 
     */
    private boolean isVirtualLevel(Map<String, Object> itemData) {
        return ! (itemData.containsKey(REPOID) && itemData.containsKey(UNITID));
    }

    private DocumentaryUnit findReferredToDocumentaryUnit(Map<String, Object> itemData) throws ItemNotFound {
        if(itemData.containsKey(REPOID) && itemData.containsKey(UNITID)){
            String repositoryid = itemData.get(REPOID).toString();
            String unitid = itemData.get(UNITID).toString();
            Repository repository = manager.getFrame(repositoryid, Repository.class);
            for (DocumentaryUnit unit : repository.getAllCollections()){
                logger.debug(unit.getIdentifier() + " " + unit.getId() + " "+ unitid);
                if(unit.getIdentifier().equals(unitid))
                    return unit;
            }
            throw new ItemNotFound(String.format("No item %s found in repo %s", unitid, repositoryid) );
        }
        throw new ItemNotFound(String.format("Apparently no repositoryid/unitid combo given"));
        
    }
}
