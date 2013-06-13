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
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.persistance.Bundle;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the ukrainian file has both repository info and collection info per row.
 * so it is done in two steps: first the UkrainianRepoImporter imports the repository info, en then passes the map 
 * to the UkranianUnitImporter which imports the DocumentaryUnit and related stuff.
 * 
 * @author linda
 */
public class UkrainianRepoImporter extends XmlImporter<Object> {

    private XmlImportProperties p;
    private static final Logger logger = LoggerFactory.getLogger(UkrainianRepoImporter.class);

    public UkrainianRepoImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        p = new XmlImportProperties("ukraine_repo.properties");
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        logger.debug("-----------------------------------");
        Bundle unit = new Bundle(EntityClass.REPOSITORY, extractUnit(itemData));


        unit = unit.withRelation(Description.DESCRIBES, new Bundle(EntityClass.REPOSITORY_DESCRIPTION, extractUnitDescription(itemData, "uk")));
        
        IdGenerator generator = IdentifiableEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.REPOSITORY, permissionScope, unit);
        boolean exists = manager.exists(id);
        logger.debug("already exists: " + exists);
        Repository repo = persister.createOrUpdate(unit.withId(id), Repository.class);
        if (!permissionScope.equals(SystemScope.getInstance())) {
            repo.setPermissionScope(permissionScope);
        }

        if (exists) {
            for (ImportCallback cb : updateCallbacks) {
                cb.itemImported(repo);
            }
        } else {
            for (ImportCallback cb : createCallbacks) {
                cb.itemImported(repo);
            }
        }
        
        UkrainianUnitImporter unitImporter = new UkrainianUnitImporter((FramedGraph<Neo4jGraph>)framedGraph, repo, log);
        unitImporter.importItem(itemData);
        
        return repo;

    }


    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = new HashMap<String, Object>();
        if (itemData.containsKey("repository_code")) {
            item.put(IdentifiableEntity.IDENTIFIER_KEY, itemData.get("repository_code"));
        } else {
            logger.error("missing identifier");
        }
        return item;
    }

    public Map<String, Object> constructDateMap(Map<String, Object> itemData) {
        Map<String, Object> item = new HashMap<String, Object>();
        String origDate = itemData.get("dates").toString();
        if (origDate.indexOf(",,") > 0) {
            String[] dates = itemData.get("dates").toString().split(",,");
            item.put(DatePeriod.START_DATE, dates[0]);
            item.put(DatePeriod.END_DATE, dates[1]);
        } else {
            item.put(DatePeriod.START_DATE, origDate);
            item.put(DatePeriod.END_DATE, origDate);
        }
        return item;
    }

    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, String language) {
        Map<String, Object> item = new HashMap<String, Object>();


        for (String key : itemData.keySet()) {
            if ((!key.equals("identifier")) && 
                    (p.getProperty(key) != null) &&
                    !(p.getProperty(key).equals("IGNORE")) && 
                    (!key.equals("dates")) && 
                    (!key.equals("language_of_description"))
                    ) {
                if (!p.containsProperty(key)) {
                    SaxXmlHandler.putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    SaxXmlHandler.putPropertyInGraph(item, p.getProperty(key), itemData.get(key).toString());
                }
            }

        }
        //replace the language from the itemData with the one specified in the param
        SaxXmlHandler.putPropertyInGraph(item, Description.LANGUAGE_CODE, language);
        return item;
    }

    @Override
    public Iterable<Map<String, Object>> extractDates(Object data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
