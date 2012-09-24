package eu.ehri.project.importers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.EntityBundle;

public class BaseImporter<T> implements Importer<T> {
    private Object repositoryId;
    private FramedGraph<Neo4jGraph> framedGraph;

    public BaseImporter(FramedGraph<Neo4jGraph> framedGraph, Object repositoryId) {
        this.repositoryId = repositoryId;
        this.framedGraph = framedGraph;
    }

    public void importDocumentaryUnit(T data) throws Exception {
        // TODO Auto-generated method stub

    }

    public EntityBundle<DocumentaryUnit> extractDocumentaryUnit(T data)
            throws ValidationError {
        return new BundleFactory<DocumentaryUnit>().buildBundle(
                new HashMap<String, Object>(), DocumentaryUnit.class);
    }

    public List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(
            T data) {
        return new LinkedList<EntityBundle<DocumentDescription>>();
    }

    public List<EntityBundle<DocumentaryUnit>> extractParent(T data) {
        return new LinkedList<EntityBundle<DocumentaryUnit>>();
    }

    public List<EntityBundle<Authority>> extractAuthorities(T data) {
        return new LinkedList<EntityBundle<Authority>>();
    }

    public List<EntityBundle<DatePeriod>> extractDates(T data) {
        return new LinkedList<EntityBundle<DatePeriod>>();
    }

    public void importDetails(T data) throws Exception {
        EntityBundle<DocumentaryUnit> unit = extractDocumentaryUnit(data);
        BundleDAO<DocumentaryUnit> persister = new BundleDAO<DocumentaryUnit>(framedGraph);
        DocumentaryUnit frame = persister.insert(unit);
        
        // Save DatePeriods
        {
            BundleDAO<DatePeriod> datePersister = new BundleDAO<DatePeriod>(framedGraph);
            for (EntityBundle<DatePeriod> dpb : extractDates(data)) {
                frame.addDatePeriod(datePersister.insert(dpb));
            }            
        }

        // Save Descriptions
        {
            BundleDAO<DocumentDescription> descPersister = new BundleDAO<DocumentDescription>(framedGraph);
            for (EntityBundle<DocumentDescription> dpb : extractDocumentDescriptions(data)) {
                frame.addDescription(descPersister.insert(dpb));
            }            
        }

    }    
}
