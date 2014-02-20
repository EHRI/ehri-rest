package eu.ehri.project.importers;

import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.*;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.impl.CrudViews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import EAC for a given repository into the database.
 *
 * @author lindar
 */
public class EacImporter extends EaImporter {

    private static final Logger logger = LoggerFactory.getLogger(EacImporter.class);
    private final Accessor userProfile;

    /**
     * Construct an EacImporter object.
     *
     * @param framedGraph
     * @param permissionScope
     * @param log
     */
    public EacImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        try {
            userProfile = manager.getFrame(log.getActioner().getId(), Accessor.class);
        } catch (ItemNotFound itemNotFound) {
            throw new RuntimeException("Unable to find accessor with given id: " + log.getActioner().getId());
        }
    }

    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        return importItem(itemData);
    }

    /**
     * @param itemData
     * @throws ValidationError
     */
    @Override
    public HistoricalAgent importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = new BundleDAO(framedGraph, permissionScope.idPath());

        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));

        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        // Add dates and descriptions to the bundle since they're @Dependent
        // relations.
        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }

        //add the address to the description bundle
        Map<String, Object> address = extractAddress(itemData);
        if (!address.isEmpty()) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_ADDRESS, new Bundle(EntityClass.ADDRESS, address));
        }

        Map<String, Object> unknowns = extractUnknownProperties(itemData);
        if (!unknowns.isEmpty()) {
            logger.debug("Unknown Properties found");
            descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, new Bundle(EntityClass.UNKNOWN_PROPERTY, unknowns));
        }

        for (Map<String, Object> dpb : extractMaintenanceEvent(itemData, itemData.get("objectIdentifier").toString())) {
            logger.debug("maintenance event found");
            //dates in maintenanceEvents are no DatePeriods, they are not something to search on
            descBundle = descBundle.withRelation(Ontology.HAS_MAINTENANCE_EVENT, new Bundle(EntityClass.MAINTENANCE_EVENT, dpb));
        }

        for (Map<String, Object> rel : extractRelations(itemData)) {
            logger.debug("relation found");
            descBundle = descBundle.withRelation(Ontology.HAS_ACCESS_POINT, new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP, rel));
        }

        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();

        if (mutation.created()) {
            solveUndeterminedRelationships(frame, descBundle);
        }

        // There may or may not be a specific scope here...
        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            frame.setAuthoritativeSet(framedGraph.frame(permissionScope.asVertex(), AuthoritativeSet.class));
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    @Override
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> description = new HashMap<String, Object>();
        for (String key : itemData.keySet()) {
            if (key.equals("descriptionIdentifier")) {
                description.put(Ontology.IDENTIFIER_KEY, itemData.get(key));
            } else if (key.startsWith("name")) {
                Object name = itemData.get(key);
                if (name instanceof List) {
                    for (Object nameentry : (List) name) {
                        if (nameentry instanceof Map) {
                            String nameType = (String) ((Map<String, Object>) nameentry).get("name/nameType");
                            String namePart = (String) ((Map<String, Object>) nameentry).get("name/namePart");
                            if (namePart != null && nameType != null) {
                                if (nameType.equals("authorized"))
                                    description.put(Ontology.NAME_KEY, namePart);
                                else if (nameType.equals("parallel"))
                                    description.put("parallelFormsOfName", namePart);
                                else
                                    description.put("otherFormsOfName", namePart);
                            }
                        }
                    }
                }
            } else if (!key.startsWith(SaxXmlHandler.UNKNOWN)
                    && !key.equals("objectIdentifier")
                    && !key.equals(Ontology.IDENTIFIER_KEY)
                    && !key.startsWith("maintenanceEvent")
                    && !key.startsWith("relation")
                    && !key.startsWith("address/")) {
                description.put(key, changeForbiddenMultivaluedProperties(key, itemData.get(key), entity));
            }

        }
//        assert(description.containsKey(IdentifiableEntity.IDENTIFIER_KEY));
        return description;
    }

    /**
     * Tries to resolve the undetermined relationships for IcaAtoM eac files by iterating through all UndeterminedRelationships,
     * finding the DescribedEntity meant by the 'targetUrl' in the Relationship and creating an Annotation for it.
     *
     *
     * @param frame
     * @param descBundle
     * @throws ValidationError
     */
    private void solveUndeterminedRelationships(HistoricalAgent frame, Bundle descBundle)
            throws ValidationError {
        //Try to resolve the undetermined relationships
        //we can only create the annotations after the HistoricalAgent and it Description have been added to the graph,
        //so they have id's. 
        Description histdesc = null;
        //we need the id (not the identifier) of the description, this requires some checking
        for (Description thisAgentDescription : frame.getDescriptions()) {
            //is thisAgentDescription the one we just created?
            if (thisAgentDescription.asVertex().getProperty(Ontology.IDENTIFIER_KEY).equals(descBundle.getData().get(Ontology.IDENTIFIER_KEY))) {
                histdesc = thisAgentDescription;
                break;
            }
        }
        if (histdesc == null) {
            logger.warn("newly created description not found");
        } else {
            // Put the set of relationships into a HashSet to remove duplicates.
            for (UndeterminedRelationship rel : Sets.newHashSet(histdesc.getUndeterminedRelationships())) {
                //our own ica-atom generated eac files have as target of a relation the url of the ica-atom
                //this must be matched back to descriptionUrl property in a previously created HistoricalAgentDescription
                String targetUrl = (String)rel.asVertex().getProperty(LINK_TARGET);
                Iterable<Vertex> docs = framedGraph.getVertices("descriptionUrl", targetUrl);
                if (docs.iterator().hasNext()) {
                    String annotationType = rel.asVertex().getProperty(Ontology.LINK_HAS_TYPE).toString();
                    DescribedEntity targetEntity = framedGraph.frame(docs.iterator().next(), Description.class).getEntity();
                    try {
                        Bundle linkBundle = new Bundle(EntityClass.LINK)
                                .withDataValue(Ontology.LINK_HAS_TYPE, annotationType)
                                .withDataValue(Ontology.LINK_HAS_DESCRIPTION, rel.asVertex().getProperty(Ontology.LINK_HAS_DESCRIPTION));
                        Link link = new CrudViews<Link>(framedGraph, Link.class).create(linkBundle, userProfile);
                        frame.addLink(link);
                        targetEntity.addLink(link);
                        link.addLinkBody(rel);

                        //attach the mirror Undetermined Relationship as a body to this Annotation
                        String thisUrl = descBundle.getData().get("descriptionUrl").toString();
                        for (Description targetEntityDescription : targetEntity.getDescriptions()) {
                            for (UndeterminedRelationship remoteRel : Sets.newHashSet(targetEntityDescription.getUndeterminedRelationships())) {
                                //check that both the body targeturl and the type are the same
                                if (thisUrl.equals(remoteRel.asVertex().getProperty(LINK_TARGET))
                                        && annotationType.equals(remoteRel.asVertex().getProperty(Ontology.LINK_HAS_TYPE))
                                        ) {
                                    link.addLinkBody(remoteRel);
                                }
                            }
                        }
                    } catch (IntegrityError e) {
                        logger.error(e.getMessage());
                        throw new RuntimeException(e);
                    } catch (PermissionDenied ex) {
                        logger.error(ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                } else {
                    logger.info("relation found, but target " + rel.asVertex().getProperty(LINK_TARGET) + " not in graph");
                }
            }
        }
    }
}
