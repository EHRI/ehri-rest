/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.persistance.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class PersonalitiesImporter extends XmlImporter<Object> {
    
    private XmlImportProperties p;
    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesImporter.class);
    
    public PersonalitiesImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        p = new XmlImportProperties("personalities.properties");
    }
    
    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));
        
        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));
        
        unit = unit.withRelation(Description.DESCRIBES, descBundle);
        
        IdGenerator generator = IdentifiableEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.HISTORICAL_AGENT, SystemScope.getInstance(), unit);
        boolean exists = manager.exists(id);
        HistoricalAgent frame = persister.createOrUpdate(unit.withId(id), HistoricalAgent.class);
        
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
    
    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

//    private Map<String, Object> constructDateMap(Map<String, Object> itemData) {
//        Map<String, Object> items = new HashMap<String, Object>();
//        String end = itemData.get("DateofdeathYYYY-MM-DD").toString();
//        String start = itemData.get("DateofbirthYYYY-MM-DD").toString();
//        if (start != null && start.endsWith("00-00")) {
//            start = start.substring(0, 4);
//        }
//        if (end != null && end.endsWith("00-00")) {
//            end = end.substring(0, 4);
//        }
//        if (end != null || start != null) {
//            items.put("existDate", (start != null ? start + " - " : "") + (end != null ? end : ""));
//        }
//        return items;
//    }
    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = new HashMap<String, Object>();
        if (itemData.containsKey("id")) {
            item.put(IdentifiableEntity.IDENTIFIER_KEY, itemData.get("id"));
        } else {
            logger.error("missing objectIdentifier");
        }
        return item;
    }
    
    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entityClass) {
        Map<String, Object> item = new HashMap<String, Object>();
        
        putPropertyInGraph(item, Description.NAME, itemData.get("Firstname") + (itemData.containsKey("Firstname") && itemData.containsKey("Lastname") ? " " : "") + itemData.get("Lastname"));
        for (String key : itemData.keySet()) {
            if (!key.equals("id")) {
                if (!p.containsProperty(key)) {
                    putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    putPropertyInGraph(item, p.getProperty(key), itemData.get(key).toString());
                }
            }
        }
        if (!item.containsKey(Description.LANGUAGE_CODE)) {
            putPropertyInGraph(item, Description.LANGUAGE_CODE, "en");
        }
        return item;
    }
    
    private void putPropertyInGraph(Map<String, Object> c, String property, String value) {
        String valuetrimmed = value.trim();
        if (valuetrimmed.isEmpty()) {
            return;
        }
        logger.debug("putProp: " + property + " " + value);
        
        Object propertyList;
        //TODO: if property is not multivalued, just concatenate
        if (c.containsKey(property)) {
            propertyList = c.get(property);
            if (propertyList instanceof List) {
                ((List<Object>) propertyList).add(valuetrimmed);
            } else {
                List<Object> o = new ArrayList<Object>();
                o.add(valuetrimmed);
                o.add(c.get(property));
                c.put(property, o);
            }
        } else {
            c.put(property, valuetrimmed);
        }
    }
    
    @Override
    public Iterable<Map<String, Object>> extractDates(Object data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
