/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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

        BundleDAO persister = getPersister();

        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));

        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();

        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            frame.setAuthoritativeSet(framedGraph.frame(permissionScope.asVertex(), AuthoritativeSet.class));
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = new HashMap<String, Object>();
        if (itemData.containsKey("id")) {
            item.put(Ontology.IDENTIFIER_KEY, itemData.get("id"));
        } else {
            logger.error("missing objectIdentifier");
        }
        return item;
    }

    private String getName(Map<String, Object> itemData) {
        // FIXME: This all sucks
        String firstName = (String) itemData.get("Firstname");
        String lastName = (String) itemData.get("Lastname");
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

        SaxXmlHandler.putPropertyInGraph(item, Ontology.NAME_KEY, getName(itemData));
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
        if (!item.containsKey("typeOfEntity")) {
            SaxXmlHandler.putPropertyInGraph(item, "typeOfEntity", "person");
        }
        if (!item.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            SaxXmlHandler.putPropertyInGraph(item, Ontology.LANGUAGE_OF_DESCRIPTION, "en");
        }
        return item;
    }


    /**
     * @param itemData
     * @return returns a List with Maps with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> itemData) {

        List<Map<String, Object>> l = Lists.newArrayList();
        Map<String, Object> items = Maps.newHashMap();

        String end = (String) itemData.get("DateofdeathYYYY-MM-DD");
        String start = (String) itemData.get("DateofbirthYYYY-MM-DD");

        if (start != null && start.endsWith("00-00")) {
            start = start.substring(0, 4);
        }
        if (end != null && end.endsWith("00-00")) {
            end = end.substring(0, 4);
        }
        if (end != null || start != null) {
            if (start != null)
                items.put(Ontology.DATE_PERIOD_START_DATE, start);
            if (end != null)
                items.put(Ontology.DATE_PERIOD_END_DATE, end);
            l.add(items);
        }
        return l;
    }

    @Override
    public Iterable<Map<String, Object>> extractDates(Object data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
