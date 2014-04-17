package eu.ehri.project.importers;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
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

import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 * <p/>
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author lindar
 */
public class IcaAtomEadImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(IcaAtomEadImporter.class);
    /**
     * Depth of top-level items. For reasons as-yet-undetermined in the bowels of the SamXmlHandler, top level items are
     * at depth 1 (rather than 0)
     */
    private final int TOP_LEVEL_DEPTH = 1;
    private Serializer mergeSerializer;

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public IcaAtomEadImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        mergeSerializer = new Serializer.Builder(framedGraph).dependentOnly().build();

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData
     * @throws ValidationError when the itemData does not contain an identifier for the unit.
     */
    @Override
    public DocumentaryUnit importItem(Map<String, Object> itemData, List<String> idPath) throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractDocumentaryUnit(itemData));

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
            logger.debug("relation found " + rel.get(Ontology.IDENTIFIER_KEY));
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }

        Mutation<DocumentaryUnit> mutation = persister.createOrUpdate(mergeWithPreviousAndSave(unit, descBundle),
                DocumentaryUnit.class);
//                persister.createOrUpdate(unit, DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        if (mutation.created()) {
            solveUndeterminedRelationships(frame, descBundle);
        }
        // Set the repository/item relationship
        if (idPath.isEmpty() && mutation.created()) {
            EntityClass scopeType = manager.getEntityClass(permissionScope);
            if (scopeType.equals(EntityClass.REPOSITORY)) {
                Repository repository = framedGraph.frame(permissionScope.asVertex(), Repository.class);
                frame.setRepository(repository);
                frame.setPermissionScope(repository);
            } else if (scopeType.equals(EntityClass.DOCUMENTARY_UNIT)) {
                DocumentaryUnit parent = framedGraph.frame(permissionScope.asVertex(), DocumentaryUnit.class);
                parent.addChild(frame);
                frame.setPermissionScope(parent);
            } else {
                logger.error("Unknown scope type for documentary unit: {}", scopeType);
            }
        }
        handleCallbacks(mutation);
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
    protected Bundle mergeWithPreviousAndSave(Bundle unit, Bundle descBundle) throws ValidationError {
        final String languageOfDesc = descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION);
        Bundle withIds = unit.generateIds(getPermissionScope().idPath());
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
                        return bundle.getType().equals(EntityClass.DOCUMENT_DESCRIPTION)
                                && (lang != null
                                && lang.equals(languageOfDesc));
                    }
                };
                Bundle filtered = oldBundle.filterRelations(filter);

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

    @SuppressWarnings("unchecked")
    @Override
    protected Iterable<Map<String, Object>> extractRelations(Map<String, Object> data) {
        final String REL = "Access";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String key : data.keySet()) {
            if (key.endsWith(REL)) {
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
                        relationNode.put(Ontology.UNDETERMINED_RELATIONSHIP_TYPE, "corporateBodyAccess");
                    }
                    list.add(relationNode);
                }
            }
        }
        return list;
    }

    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData, int depth) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        if (itemData.get(OBJECT_ID) != null) {
            unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_ID));
        }
        return unit;
    }

    protected Map<String, Object> extractDocumentDescription(Map<String, Object> itemData, int depth) throws ValidationError {

        Map<String, Object> unit = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (!(key.equals(OBJECT_ID) || key.startsWith(SaxXmlHandler.UNKNOWN))) {
                unit.put(key, itemData.get(key));
            }
        }
        return unit;
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void solveUndeterminedRelationships(DocumentaryUnit frame, Bundle descBundle) throws ValidationError {
        // can be used by subclasses to solve any undeterminedRelationships
    }
}
