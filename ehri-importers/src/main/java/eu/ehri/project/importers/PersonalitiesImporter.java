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
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * before importing the file: delete the columns with the reordering of the first and last name
 * add a column 'id' with a unique identifier, prefixed with EHRI-Personalities or some such.
 * 
 * @author linda
 */
public class PersonalitiesImporter extends XmlImporter<Object> {
    
    private final XmlImportProperties p = new XmlImportProperties("personalities.properties");
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalitiesImporter.class);
    
    public PersonalitiesImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));
        
        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));
        
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        unit = unit.withRelation(Description.DESCRIBES, descBundle);
        
        IdGenerator generator = EntityClass.HISTORICAL_AGENT.getIdgen();
        String id = generator.generateId(EntityClass.HISTORICAL_AGENT, permissionScope, unit);
        boolean exists = manager.exists(id);
        HistoricalAgent frame = persister.createOrUpdate(unit.withId(id), HistoricalAgent.class);

        // FIXME: Relationships will be created twice if updating.
        if (!permissionScope.equals(SystemScope.getInstance())) {
            frame.setAuthoritativeSet(framedGraph.frame(permissionScope.asVertex(), AuthoritativeSet.class));
        }
        
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

    private String getName(Map<String, Object> itemData) {
        // FIXME: This all sucks
        String firstName = (String)itemData.get("Firstname");
        String lastName = (String)itemData.get("Lastname");
        if (firstName == null && lastName == null) {
            return null;
        }
        String name = "";
        if (lastName != null) {
            name = lastName;
        }
        if (firstName != null) {
            name = firstName + " " + name;
        }
        return name;
    }
    
    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entityClass) {
        Map<String, Object> item = new HashMap<String, Object>();
        
        SaxXmlHandler.putPropertyInGraph(item, Description.NAME, getName(itemData));
        for (String key : itemData.keySet()) {
            if (!key.equals("id")) {
                if (!p.containsProperty(key)) {
                    SaxXmlHandler.putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    SaxXmlHandler.putPropertyInGraph(item, p.getProperty(key), itemData.get(key).toString());
                }
            }
            
        }
        //create all otherFormsOfName
        if(!item.containsKey("typeOfEntity")){
            SaxXmlHandler.putPropertyInGraph(item, "typeOfEntity", "person");
        }
        if (!item.containsKey(Description.LANGUAGE_CODE)) {
            SaxXmlHandler.putPropertyInGraph(item, Description.LANGUAGE_CODE, "en");
        }
        return item;
    }

    
    /**
     * 
     * @param itemData
     * @return returns a List with Maps with DatePeriod.START_DATE and DatePeriod.END_DATE values     
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> itemData) {
        Map<String, Object> items = new HashMap<String, Object>();
        String end = itemData.get("DateofdeathYYYY-MM-DD").toString();
        String start = itemData.get("DateofbirthYYYY-MM-DD").toString();
        if (start != null && start.endsWith("00-00")) {
            start = start.substring(0, 4);
        }
        if (end != null && end.endsWith("00-00")) {
            end = end.substring(0, 4);
        }
        if (end != null || start != null) {
            if(start != null)
                items.put(DatePeriod.START_DATE, start );
            if(end != null)
                items.put(DatePeriod.END_DATE,end);
        }
        List<Map<String,Object>> l = new ArrayList<Map<String,Object>>();
        l.add(items);
        return l;
    }

    @Override
    public Iterable<Map<String, Object>> extractDates(Object data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

 
    

}
