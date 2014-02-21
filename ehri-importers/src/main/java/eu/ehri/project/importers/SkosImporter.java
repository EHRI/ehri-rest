package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.Mutation;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SkosImporter extends XmlImporter<Map<String, Object>> {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SkosImporter.class);

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public SkosImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope,
            ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    /**
     * Import a single item, keeping a reference to the hierarchical depth.
     *
     * @param itemData
     * @throws ValidationError
     */
    @Override
    public Concept importItem(Map<String, Object> itemData) throws ValidationError {

        // Note pboon: 
        // What was the 'repository' and 'scope' should eventually be the Vocabulary!
        // Also note We don't have a parent here, but it could be a Broader Concept... 
        logger.info("import item with objectIdentifier: " + itemData.get("objectIdentifier"));

        Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT,
                extractConcept(itemData));
        BundleDAO persister = new BundleDAO(framedGraph, permissionScope.idPath());

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            unit = unit.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(
                    EntityClass.DATE_PERIOD, dpb));
        }
        for (Map<String, Object> dpb : extractConceptDescription(itemData)) {
            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, new Bundle(
                    EntityClass.CVOC_CONCEPT_DESCRIPTION, dpb));
        }

        Mutation<Concept> mutation = persister.createOrUpdate(unit, Concept.class);
        handleCallbacks(mutation);
        return mutation.getNode();
    }

    /**
     * The 'item' or entities to import (Described Entity?)
     *
     * @param itemData
     * @return returns a Map of Concept key-value pairs
     * @throws ValidationError
     */
    protected Map<String, Object> extractConcept(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(Ontology.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
        return unit;
    }

    /**
     * The description of the 'item' or main entities to import
     *
     * @param itemData
     * @return returns a Map of ConceptDescription key-value pairs
     * @throws ValidationError
     */
    protected Iterable<Map<String, Object>> extractConceptDescription(Map<String, Object> itemData) throws ValidationError {

// TEST
        logger.debug("itemData keys: \n" + itemData.keySet().toString());

        List<Map<String, Object>> langs = new ArrayList<Map<String, Object>>();
        Map<String, Object> unit = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            logger.debug("extract: " + key);
            if (key.equals("descriptionIdentifier")) {
                unit.put(Ontology.IDENTIFIER_KEY, itemData.get(key));
            } else if (key.equals("languageCode")) {
                if (itemData.get(key) instanceof Map) {
                    for (String language : ((Map<String, Map<String, Object>>) itemData.get(key)).keySet()) {
                        langs.add(((Map<String, Map<String, Object>>) itemData.get(key)).get(language));
                    }
                }
            } else if (!(key.equals("objectIdentifier"))) {
                unit.put(key, itemData.get(key));
            }
        }
        for (Map<String, Object> lang : langs) {
            lang.putAll(unit);
            if (unit.containsKey(Ontology.IDENTIFIER_KEY)) {
                lang.put(Ontology.IDENTIFIER_KEY, unit.get(Ontology.IDENTIFIER_KEY).toString() + lang.get("languageCode"));
            } else {
                lang.put(Ontology.IDENTIFIER_KEY, itemData.get("objectIdentifier") + "#description_" + lang.get("languageCode"));
            }

        }
        return langs;
    }

    protected <T> List<T> toList(Iterable<T> iter) {
        Iterator<T> it = iter.iterator();
        List<T> lst = new ArrayList<T>();
        while (it.hasNext()) {
            lst.add(it.next());
        }
        return lst;
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }
}
