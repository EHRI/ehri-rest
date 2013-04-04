package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgentDescription;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AddressableEntity;
import eu.ehri.project.models.base.Annotator;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.TemporalEntity;
import eu.ehri.project.models.idgen.IdentifiableEntityIdGenerator;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.views.impl.AnnotationViews;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAC for a given repository into the database.
 *
 * @author lindar
 *
 */
public class EacImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    
    /**
     * Construct an EacImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EacImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        return importItem(itemData);
    }

    /**
     *
     *
     * @param itemData
     * @throws ValidationError
     */
    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData) throws ValidationError {

        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));

        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));


        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(TemporalEntity.HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }

        //add the address to the description bundle
        Map<String, Object> address = extractAddress(itemData);
        if (!address.isEmpty()) {
            descBundle = descBundle.withRelation(AddressableEntity.HAS_ADDRESS, new Bundle(EntityClass.ADDRESS, extractAddress(itemData)));
        }

        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData, itemData.get("objectIdentifier").toString())) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Description.MUTATES, new Bundle(EntityClass.MAINTENANCE_EVENT, dpb));
        }
        
        for (Map<String, Object> rel : extractRelations(itemData)) {
            logger.debug("relation found");
            descBundle = descBundle.withRelation(Description.RELATESTO, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }

        unit = unit.withRelation(Description.DESCRIBES, descBundle);

        IdGenerator generator = IdentifiableEntityIdGenerator.INSTANCE;
        String id = generator.generateId(EntityClass.HISTORICAL_AGENT, SystemScope.getInstance(), unit);
        boolean exists = manager.exists(id);
        HistoricalAgent frame = persister.createOrUpdate(unit.withId(id), HistoricalAgent.class);

        solveUndeterminedRelationships(id, frame, descBundle);

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
     * Tries to resolve the undetermined relationships for IcaAtoM eac files by iterating through all UndeterminedRelationships,
     * finding the DescribedEntity meant by the 'targetUrl' in the Relationship and creating an Annotation for it.
     * 
     * @param id
     * @param frame
     * @param descBundle
     * @throws ValidationError 
     */
    private void solveUndeterminedRelationships(String id, HistoricalAgent frame, Bundle descBundle) throws ValidationError {
        //Try to resolve the undetermined relationships
        //we can only create the annotations after the HistoricalAgent and it Description have been added to the graph,
        //so they have id's. 
        AnnotationViews ann = new AnnotationViews(framedGraph, permissionScope);
        Description histdesc = null;
        //we need the id (not the identifier) of the description, this requires some checking
        //is thisAgentDescription the one we just created?
        for (Description thisAgentDescription : frame.getDescriptions()) {
            //is thisAgentDescription the one we just created?
            if (thisAgentDescription.asVertex().getProperty(IdentifiableEntity.IDENTIFIER_KEY).equals(descBundle.getData().get(IdentifiableEntity.IDENTIFIER_KEY))) {
                histdesc = thisAgentDescription;
                break;
            }
        }
        if (histdesc == null) {
            logger.warn("newly created description not found");
        } else {
            for (UndeterminedRelationship rel : histdesc.getUndeterminedRelationships()) {
                //our own ica-atom generated eac files have as target of a relation the url of the ica-atom
                //this must be matched back to descriptionUrl property in a previously created HistoricalAgentDescription
                String targetUrl = rel.asVertex().getProperty(ANNOTATION_TARGET).toString();
                Iterable<Vertex> docs = framedGraph.getVertices("descriptionUrl", targetUrl);
                if (docs.iterator().hasNext()) {
                    DescribedEntity targetEntity = framedGraph.frame(docs.iterator().next(), Description.class).getEntity();
                    try {
                        Map<String, Object> annotationMap = new HashMap<String, Object>();
                        annotationMap.put(Annotation.ANNOTATION_TYPE, rel.asVertex().getProperty(Annotation.ANNOTATION_TYPE));
                        annotationMap.put(Annotation.NOTES_BODY, rel.asVertex().getProperty(Annotation.NOTES_BODY));

                        Annotation annotation = ann.createLink(id, rel.getId(), new Bundle(EntityClass.ANNOTATION, annotationMap), importUser);
                        targetEntity.addAnnotation(annotation);
                    } catch (ItemNotFound ex) {
                        logger.error(ex.getMessage());
                    } catch (PermissionDenied ex) {
                        logger.error(ex.getMessage());
                    }
                } else {
                    logger.info("relation found, but target " + rel.asVertex().getProperty(ANNOTATION_TARGET) + " not in graph");
                }

            }

        }
    }

    
}
