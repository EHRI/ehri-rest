package eu.ehri.project.importers;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.idgen.IdGenerator;
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
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c,c01-12 levels.
 *
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author lindar
 *
 */
public class BundesarchiveEadImporter extends EadImporter {

    private static final Logger logger = LoggerFactory.getLogger(BundesarchiveEadImporter.class);
    /**
     * Depth of top-level items. For reasons as-yet-undetermined in the bowels of the SamXmlHandler, top level items are
     * at depth 1 (rather than 0)
     */
    private final int TOP_LEVEL_DEPTH = 1;

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public BundesarchiveEadImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData
     * @param idPath The parent id path, not including the overall scope
     * @throws ValidationError
     */
    @Override
    public DocumentaryUnit importItem(Map<String, Object> itemData, List<String> idPath)
            throws ValidationError {

        BundleDAO persister = getPersister(idPath);

        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractDocumentaryUnit(itemData));
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
        for (Map<String, Object> rel : extractRelations(itemData)) {//, (String) unit.getData().get(IdentifiableEntity.IDENTIFIER_KEY)
            logger.debug("relation found " + rel.get(Ontology.IDENTIFIER_KEY));
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }
        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }
        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        if (unit.getDataValue(Ontology.IDENTIFIER_KEY) == null) {
            throw new ValidationError(unit, Ontology.IDENTIFIER_KEY, "Missing identifier");
        } else {
            logger.error("identifier Key: "+unit.getDataValue(Ontology.IDENTIFIER_KEY).toString());
        }
        IdGenerator generator = EntityClass.DOCUMENTARY_UNIT.getIdgen();
        String id = generator.generateId(Iterables.concat(permissionScope.idPath(), idPath), unit);
        if (id.equals(permissionScope.getId())) {
            throw new RuntimeException("Generated an id same as scope: " + unit.getData());
        }

        logger.debug("Generated ID: " + id + " (" + permissionScope.getId() + ")");

        Mutation<DocumentaryUnit> mutation =
                persister.createOrUpdate(unit.withId(id), DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();

        // Set the repository/item relationship
        //TODO: figure out another way to determine we're at the root, so we can get rid of the depth param
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

    @Override
    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData, int depth) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        if (itemData.get(OBJECT_ID) != null) {
            unit.put(Ontology.IDENTIFIER_KEY, itemData.get(OBJECT_ID));
        }
        return unit;
    }

    @Override
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
}
