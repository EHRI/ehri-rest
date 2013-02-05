package eu.ehri.project.importers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.idgen.AccessibleEntityIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;

/**
 *
 */
public class SkosImporter extends XmlCVocImporter<Map<String, Object>> {

    /**
     * Construct an EadImporter object.
     *
     * @param framedGraph
     * @param repository
     * @param log
     */
    public SkosImporter(FramedGraph<Neo4jGraph> framedGraph, Agent repository,
            ImportLog log) {
        super(framedGraph, repository, log);
    }

    /**
     * Import a single item, keeping a reference to the hierarchical depth.
     *
     * @param itemData
     * @param depth
     * @throws ValidationError
     */
    public Concept importItem(Map<String, Object> itemData,
            int depth) throws ValidationError {

    	// Note pboon: 
    	// What was the 'repository' and 'scope' should eventually be the Vocabulary!
    	// Also note We don't have a parent here, but it could be a Broader Concept... 
 System.out.println("import item with objectIdentifier: " + itemData.get("objectIdentifier"));	
    	
        Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT,
        		extractConcept(itemData, depth));
        BundleDAO persister = new BundleDAO(framedGraph, repository);

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            unit = unit.withRelation(TemporalEntity.HAS_DATE, new Bundle(
                    EntityClass.DATE_PERIOD, dpb));
        }
        for (Map<String, Object> dpb : extractConceptDescription(itemData)) {
            unit = unit.withRelation(Description.DESCRIBES, new Bundle(
                    EntityClass.CVOC_CONCEPT_DESCRIPTION, dpb));
        }

        //PermissionScope scope = parent != null ? parent : repository;
        PermissionScope scope = repository;
        
        IdGenerator generator = AccessibleEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.CVOC_CONCEPT, scope,
                    unit.getData());
        boolean exists = manager.exists(id);
        Concept frame = persister.createOrUpdate(unit.withId(id),
        		Concept.class);

        // Set the repository/item relationship
        //frame.setAgent(repository); // SHOULD set the Vocabulary at some point!
        
        frame.setPermissionScope(scope);
        // Set the parent child relationship
        //if (parent != null)
        //    parent.addChild(frame);

        // Run creation callbacks for the new item...
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

    /**
     * The 'item' or entities to import (Described Entity?)
     * 
     * @param itemData
     * @param depth
     * @return
     * @throws ValidationError
     */
    protected Map<String, Object> extractConcept(Map<String, Object> itemData, int depth) throws ValidationError {
        Map<String, Object> unit = new HashMap<String, Object>();
        unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get("objectIdentifier"));
        return unit;
    }

    /**
     * The description of the 'item' or main entities to import
     * 
     * @param itemData
     * @return
     * @throws ValidationError
     */
    protected Iterable<Map<String, Object>> extractConceptDescription(Map<String, Object> itemData) throws ValidationError {
 
// TEST
System.out.println("itemData keys: \n" + itemData.keySet().toString());

    	List<Map<String, Object>> langs = new ArrayList<Map<String, Object>>();
        Map<String, Object> unit = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            System.out.println("extract: " + key);
            if (key.equals("descriptionIdentifier")) {
                unit.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get(key));
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
            if (unit.containsKey(AccessibleEntity.IDENTIFIER_KEY)) {
                lang.put(AccessibleEntity.IDENTIFIER_KEY, unit.get(AccessibleEntity.IDENTIFIER_KEY).toString() + lang.get("languageCode"));
            } else {
                lang.put(AccessibleEntity.IDENTIFIER_KEY, itemData.get("objectIdentifier") + "#description_" + lang.get("languageCode"));
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


}
