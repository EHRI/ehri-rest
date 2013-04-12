package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.persistance.Bundle;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAD for a given repository into the database. Due to the laxness of the EAD standard this is a fairly complex
 * procedure. An EAD a single entity at the highest level of description or multiple top-level entities, with or without
 * a hierarchical structure describing their child items. This means that we need to recursively descend through the
 * archdesc and c01-12 levels.
 *
 * TODO: Extensive cleanups, optimisation, and rationalisation.
 *
 * @author lindar
 *
 */
public class IcaAtomEadImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(IcaAtomEadImporter.class);

    /**
     * Depth of top-level items. For reasons as-yet-undetermined in the bowels
     * of the SamXmlHandler, top level items are at depth 1 (rather than 0)
     */
    private int TOP_LEVEL_DEPTH = 1;

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public IcaAtomEadImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);

    }

    /**
     * Import a single archdesc or c01-12 item, keeping a reference to the hierarchical depth.
     *
     * @param itemData
     * @param depth
     * @throws ValidationError
     */
    @Override
    public DocumentaryUnit importItem(Map<String, Object> itemData, int depth)
            throws ValidationError {
//        BundleDAO persister = new BundleDAO(framedGraph, permissionScope);
        Bundle unit = new Bundle(EntityClass.DOCUMENTARY_UNIT, extractDocumentaryUnit(itemData));
        Bundle descBundle = new Bundle(EntityClass.DOCUMENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.DOCUMENT_DESCRIPTION));
        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle=descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        
        unit=unit.withRelation(Description.DESCRIBES, descBundle);

        if (unit.getDataValue(DocumentaryUnit.IDENTIFIER_KEY) == null) {
            throw new ValidationError(unit, DocumentaryUnit.IDENTIFIER_KEY, "Missing identifier");
        }
        IdGenerator generator = IdentifiableEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.DOCUMENTARY_UNIT, permissionScope, unit);
        if (id.equals(permissionScope.getId())) {
            throw new RuntimeException("Generated an id same as scope: " + unit.getData());
        }
        boolean exists = manager.exists(id);
        DocumentaryUnit frame = persister.createOrUpdate(unit.withId(id), DocumentaryUnit.class);

        // Set the repository/item relationship
        //TODO: figure out another way to determine we're at the root, so we can get rid of the depth param
        if (depth == TOP_LEVEL_DEPTH) {
            Repository repository = framedGraph.frame(permissionScope.asVertex(), Repository.class);
            frame.setRepository(repository);
        }
        frame.setPermissionScope(permissionScope);
        
        
        if (exists) {
            for (ImportCallback cb : updateCallbacks) {
                cb.itemImported(frame);
            }
        } else {
            for (ImportCallback cb : createCallbacks) {
                cb.itemImported(frame);
            }
        }
        return frame;


    }

    protected Map<String, Object> extractDocumentaryUnit(Map<String, Object> itemData, int depth) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        if (itemData.get(OBJECT_ID) != null) {
            unit.put(IdentifiableEntity.IDENTIFIER_KEY, itemData.get(OBJECT_ID));
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
}
