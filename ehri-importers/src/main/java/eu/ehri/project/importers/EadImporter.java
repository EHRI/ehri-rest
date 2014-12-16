package eu.ehri.project.importers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AbstractUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.impl.CrudViews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 *
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 *
 */
public class EadImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EadImporter.class);
    //the EadImporter can import ead as DocumentaryUnits, the default, or overwrite those and create VirtualUnits instead.
    private EntityClass unitEntity = EntityClass.DOCUMENTARY_UNIT;
    private Serializer mergeSerializer;
    public static final String ACCESS_POINT = "AccessPoint";
    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EadImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        mergeSerializer = new Serializer.Builder(framedGraph).dependentOnly().build();
    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
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

        // extractDocumentaryUnit does not throw ValidationError on missing ID
        Bundle unit = new Bundle(unitEntity, extractDocumentaryUnit(itemData));
        
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
            for(String s : rel.keySet()){
                logger.debug(s);
            }
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }

        Mutation<DocumentaryUnit> mutation =
                persister.createOrUpdate(mergeWithPreviousAndSave(unit, descBundle, idPath), DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        // Set the repository/item relationship
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(permissionScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                Repository repository = framedGraph.frame(permissionScope.asVertex(), Repository.class);
                frame.setRepository(repository);
                frame.setPermissionScope(repository);
            } else if (scopeType.equals(unitEntity)) {
                DocumentaryUnit parent = framedGraph.frame(permissionScope.asVertex(), DocumentaryUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else {
                logger.error("Unknown scope type for documentary unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);
        logger.debug("============== "+frame.getIdentifier()+" created:" + mutation.created());
        if (mutation.created()) {
            solveUndeterminedRelationships(frame, descBundle);
        }
        return frame;


    }
/**
     * finds any bundle in the graph with the same ObjectIdentifier.
     * if it exists it replaces the Description in the given language, else it just saves it
     *
     * @param unit       - the DocumentaryUnit to be saved
     * @param descBundle - the documentsDescription to replace any previous ones with this language
     * @return A bundle with description relationships merged.
     * @throws ValidationError
     */

    protected Bundle mergeWithPreviousAndSave(Bundle unit, Bundle descBundle, List<String> idPath) throws ValidationError {
        final String languageOfDesc = descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION);
        final String thisSourceFileId = descBundle.getDataValue(Ontology.SOURCEFILE_KEY);
        /*
         * for some reason, the idpath from the permissionscope does not contain the parent documentary unit.
         * TODO: so for now, it is added manually
         */
        List<String> lpath = new ArrayList<String>();
        for(String p : getPermissionScope().idPath()){
            lpath.add(p);
        }
        for(String p : idPath){
            lpath.add(p);
        }
        Bundle withIds = unit.generateIds(lpath);                
        
        if (manager.exists(withIds.getId())) {
            try {
                //read the current item’s bundle
                Bundle oldBundle = mergeSerializer
                        .vertexFrameToBundle(manager.getVertex(withIds.getId()));
                //filter out dependents that a) are descriptions, b) have the same language/code
                Bundle.Filter filter = new Bundle.Filter() {
                    @Override
                    public boolean remove(String relationLabel, Bundle bundle) {
                        String lang = bundle.getDataValue(Ontology.LANGUAGE);
                        String oldSourceFileId = bundle.getDataValue(Ontology.SOURCEFILE_KEY);
                        return bundle.getType().equals(EntityClass.DOCUMENT_DESCRIPTION)
                                && (lang != null
                                && lang.equals(languageOfDesc)
                                && (oldSourceFileId != null && oldSourceFileId.equals(thisSourceFileId))
                                );
                    }
                };
                Bundle filtered = oldBundle.filterRelations(filter);
                
                //if this desc-id already exists, but with a different sourceFileId, 
                //change the desc-id
                String defaultDescIdentifier= withIds.getId()+"-"+languageOfDesc.toLowerCase();
                String newDescIdentifier=withIds.getId()+"-"+thisSourceFileId.toLowerCase().replace("#", "-");
                if(manager.exists(newDescIdentifier)){
                        descBundle=descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                } else if(manager.exists(defaultDescIdentifier)){
                    Bundle oldDescBundle = mergeSerializer
                        .vertexFrameToBundle(manager.getVertex(defaultDescIdentifier));
                    //if the previous had NO sourcefile_key OR it was different:
                    if(oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY) == null
                            || ! thisSourceFileId.equals(oldDescBundle.getDataValue(Ontology.SOURCEFILE_KEY).toString())){
                        descBundle=descBundle.withDataValue(Ontology.IDENTIFIER_KEY, newDescIdentifier);
                        logger.info("other description found, creating new description id: " + descBundle.getDataValue(Ontology.IDENTIFIER_KEY).toString());
                    }
                }

                return withIds.withRelations(filtered.getRelations())
                        .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

            } catch (SerializationError ex) {
                throw new ValidationError(unit, "serialization error", ex.getMessage());
            } catch (ItemNotFound ex) {
                throw new ValidationError(unit, "item not found exception", ex.getMessage());
            }
        } else {
            return unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }
    }

    /**
     * subclasses can override this method to cater to their special needs for UndeterminedRelationships
     * by default, it expects something like this in the original EAD:
     * 
     * <persname source="terezin-victims" authfilenumber="PERSON.ITI.1514982">Kien,
                        Leonhard (* 11.5.1886)</persname>
     *
     * it works in unison with the extractRelations() method. 
     * 
                        * 
     * @param unit
     * @param descBundle - not used
     * @throws ValidationError 
     */
    protected void solveUndeterminedRelationships(DocumentaryUnit unit, Bundle descBundle) throws ValidationError {
        //Try to resolve the undetermined relationships
        //we can only create the annotations after the DocumentaryUnit and its Description have been added to the graph,
        //so they have id's. 
        for (Description unitdesc : unit.getDescriptions()) {
            // Put the set of relationships into a HashSet to remove duplicates.
            for (UndeterminedRelationship rel : Sets.newHashSet(unitdesc.getUndeterminedRelationships())) {
                /*
                 * the wp2 undetermined relationship that can be resolved have a 'cvoc' and a 'concept' attribute.
                 * they need to be found in the vocabularies that are in the graph
                 */
                if (rel.asVertex().getPropertyKeys().contains("cvoc")) {
                    String cvoc_id = (String) rel.asVertex().getProperty("cvoc");
                    String concept_id = (String) rel.asVertex().getProperty("concept");
                    if(concept_id == null){
                      concept_id = (String) rel.asVertex().getProperty("target");
                    }
                    logger.debug("cvoc:"+cvoc_id + "  concept:" + concept_id);
                    Vocabulary vocabulary;
                    try {
                        vocabulary = manager.getFrame(cvoc_id, Vocabulary.class);
                        for (Concept concept : vocabulary.getConcepts()) {
                        logger.debug("*********************" + concept.getId() + " " + concept.getIdentifier());
                        if (concept.getIdentifier().equals(concept_id)) {
                            try {
                                Bundle linkBundle = new Bundle(EntityClass.LINK)
                                        .withDataValue(Ontology.LINK_HAS_TYPE, rel.asVertex().getProperty("type").toString())
                                        .withDataValue(Ontology.LINK_HAS_DESCRIPTION, RESOLVED_LINK_DESC);
                                UserProfile user = manager.getFrame(this.log.getActioner().getId(), UserProfile.class);
                                Link link = new CrudViews<Link>(framedGraph, Link.class).create(linkBundle, user);
                                unit.addLink(link);
                                concept.addLink(link);
                                link.addLinkBody(rel);
                            } catch (PermissionDenied ex) {
                                logger.error(ex.getMessage());
                            } catch (IntegrityError ex) {
                                logger.error(ex.getMessage());
                            }

                        }

                    }
                    } catch (ItemNotFound ex) {
                        logger.error("Vocabulary with id " + cvoc_id +" not found. "+ex.getMessage());
                    }
                    
                }else{
                    logger.debug("no cvoc found");
                }
                
            }
        }
    }

    @SuppressWarnings("unchecked")
   @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        final String REL = "relation";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : data.keySet()) {
            if (key.equals(REL)) {
                //name identifier
                for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                    Map<String, Object> relationNode = new HashMap<String, Object>();
                    if(origRelation.containsKey("type")){
                            //try to find the original identifier
                            relationNode.put(LINK_TARGET, origRelation.get("concept"));
                            //try to find the original name
                            relationNode.put(Ontology.NAME_KEY, origRelation.get("name"));
                            relationNode.put("cvoc", origRelation.get("cvoc"));
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, origRelation.get("type"));
                    } else {
                        relationNode.put(Ontology.NAME_KEY, origRelation.get(REL));
                    }
                    if (!relationNode.containsKey(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)) {
                        logger.debug("relationNode without type: "+relationNode.get(Ontology.NAME_KEY));
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccess");
                    }
                    list.add(relationNode);
                }
            }
            else 
    if (key.endsWith(ACCESS_POINT)) {

                logger.debug(key + data.get(key).getClass());
                if (data.get(key) instanceof List) {
                    for (Object o : (List) data.get(key)) {
                        logger.debug("" + o.getClass());
                    }
                    //type, targetUrl, targetName, notes
                    for (Map<String, Object> origRelation : (List<Map<String, Object>>) data.get(key)) {
                        if (origRelation.isEmpty()) {
                            break;
                        }
                        Map<String, Object> relationNode = new HashMap<String, Object>();
                        for (String eventkey : origRelation.keySet()) {
                            if (eventkey.endsWith(ACCESS_POINT)) {
                                relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, eventkey.substring(0, eventkey.indexOf("Point")));
                                relationNode.put(Ontology.NAME_KEY, origRelation.get(eventkey));
//logger.debug("------------------" + eventkey.substring(0, eventkey.indexOf("Point")) + ": "+ origRelation.get(eventkey));                            
                            } else {
                                relationNode.put(eventkey, origRelation.get(eventkey));
                            }
                        }
                        if (!relationNode.containsKey(Ontology.UNDETERMINED_RELATIONSHIP_TYPE)) {
                            relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccess");
                        }
                        //if no name is given, it was apparently an empty <controlaccess> tag?
                        if (relationNode.containsKey(Ontology.NAME_KEY)) {
                            list.add(relationNode);
                        }
                    }
                } else {
                        Map<String, Object> relationNode = new HashMap<String, Object>();
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, key.substring(0, key.indexOf("Point")));
                        relationNode.put(Ontology.NAME_KEY, data.get(key));
                        list.add(relationNode);
                    
                }
            }
        }
        return list;
    }

    /**
     * Creates a Map containing properties of a Documentary Unit.
     * These properties are the unit's identifiers.
     * @param itemData Map of all extracted information
     * @param depth depth of node in the tree
     * @return a Map representing a Documentary Unit node
     * @throws ValidationError
     */
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData, int depth) throws ValidationError {
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
    
    /**
     * Creates a Map containing properties of a Documentary Unit.
     * These properties are the unit's identifiers.
     * @param itemData Map of all extracted information
     * @return a Map representing a Documentary Unit node
     * @throws ValidationError
     */
    @Override
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData) throws ValidationError {
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

    
    public void importAsVirtualCollection(){
      unitEntity = EntityClass.VIRTUAL_UNIT;
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        return importItem(itemData, new Stack<String>());
    }
}
