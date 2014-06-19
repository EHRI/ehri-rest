/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.impl.CrudViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * imports an ead file and not only constructs the DocumentaryUnits and DocumentsDescription for it, as usual, but a;so a VirtualUnit structure mirroring the DocUnits.
 * These VirtualUnits link to the DocDescs and function as the target of the constructed Links.
 * 
 * @author linda
 */
public class EadIntoVirtualCollectionImporter extends IcaAtomEadImporter {

    private static final Logger logger = LoggerFactory.getLogger(EadIntoVirtualCollectionImporter.class);
    private final Accessor userProfile;
    public static final String WP2AUTHOR = "EHRI";
    public static final String PROPERTY_AUTHOR = "authors";
    private VirtualUnit virtualCollection;
    private PermissionScope virtualPermissionScope;

    
    private List<String> idPath;
    public static final String VIRTUAL_PREFIX = "virtual";
    
    public EadIntoVirtualCollectionImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        try {
            userProfile = manager.getFrame(log.getActioner().getId(), Accessor.class);
        } catch (ItemNotFound ex) {
            throw new RuntimeException("Unable to find accessor with given id: " + log.getActioner().getId());
        }
    }
    
    @Override
    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entity) {
        Map<String, Object> map = super.extractUnitDescription(itemData, entity);
        map.put(PROPERTY_AUTHOR, WP2AUTHOR);    
        return map;
    }
    
    @Override
    public DocumentaryUnit importItem(Map<String, Object> itemData, List<String> idPath) throws ValidationError {
        this.idPath = idPath;
        return super.importItem(itemData, idPath);
    }


    @Override
    public BundleDAO getPersister() {
        return new BundleDAO(framedGraph,
                Iterables.concat(virtualPermissionScope.idPath(), idPath));
    }
    
    /**
     * Tries to resolve the undetermined relationships for Wp2 ead files by iterating through all
     * UndeterminedRelationships, finding the DescribedEntity meant by the 'targetUrl' in the Relationship and creating
     * a Link for it.
     * 
     * since this is in the context of a VirtualCollection, the Link is not added to the DocumentaryUnit but to the VirtualUnit
     *
     * @param unit
     * @param descBundle
     * @throws ValidationError
     */
    @Override
    protected void solveUndeterminedRelationships(DocumentaryUnit unit, Bundle descBundle) throws ValidationError {
        //always create a VirtualUnit
        BundleDAO persister = getPersister();//new BundleDAO(framedGraph, virtualPermissionScope.idPath());
        Map<String, Object> virtualUnitMap = new HashMap<String, Object>();
        virtualUnitMap.put(Ontology.IDENTIFIER_KEY, VIRTUAL_PREFIX+unit.getIdentifier());
        Bundle virtualUnitBundle = new Bundle(EntityClass.VIRTUAL_UNIT, virtualUnitMap);
        Mutation<VirtualUnit> mutation = persister.createOrUpdate(virtualUnitBundle, VirtualUnit.class);
        VirtualUnit virtualUnit = mutation.getNode();
       
        
        //  add all descBundle descriptions to the VirtualUnit.     
        for (DocumentDescription unitdesc : unit.getDocumentDescriptions()) {
            String lang = unitdesc.getLanguageOfDescription();
            if(lang != null && lang.equals(descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION))){
               virtualUnit.addReferencedDescription(unitdesc);
            } else {
                logger.error("language of found description is " + lang + " which is not in the descBundle");
            }
        }
        
        if (virtualCollection != null) {
            if (mutation.created()) {
                logger.error("virtualPermissionScope idPath: " + virtualPermissionScope.idPath());
                if (idPath.isEmpty() && manager.getEntityClass(permissionScope).equals(EntityClass.REPOSITORY)) {
                    virtualCollection.addChild(virtualUnit);
                    virtualUnit.setPermissionScope(virtualPermissionScope);
                    virtualPermissionScope = virtualUnit;
                    logger.error("repo " + virtualPermissionScope.idPath());
                } else {
                    logger.error(" not the top level");
                    //the parent is created last, so find all the children of this unit, they already exist
                    Iterable<Frame> children = unit.getContainedItems();
                    int count=0;
                    for(Frame childframe : children){
                        count++;
                        VirtualUnit child = framedGraph.frame(childframe.asVertex(), VirtualUnit.class);
                        logger.debug(child.getIdentifier() + " has parent " + child.getParent().getIdentifier());
                        if(child.getParent().getIdentifier().equals(virtualCollection.getIdentifier())){
                            logger.error(" ----------- found child " + child.getIdentifier() + " of parent "  + virtualUnit.getIdentifier());
                            virtualUnit.addChild(child);
                            child.setPermissionScope(virtualUnit);
                        }
                            
                    }
                    logger.error("containedItems: " + count);
//                    Iterable<DocumentDescription> parentDesc = docParent.getDocumentDescriptions();
//                    for (DocumentDescription unitdesc : parentDesc) {
//                        String lang = unitdesc.getLanguageOfDescription();
//                        if (lang != null && lang.equals(descBundle.getDataValue(Ontology.LANGUAGE_OF_DESCRIPTION))) {
//                            //found the description of the parent of the current DocUnit. 
//                            //let's find the attached VirtualUnit:
//                            Iterable<Edge> virtUnitsEdges = unitdesc.asVertex().getEdges(Direction.IN, Ontology.VC_DESCRIBED_BY);
//                            for (Edge e : virtUnitsEdges) {
//                                parent = framedGraph.frame(e.getVertex(Direction.OUT), VirtualUnit.class);
//                                parent.addChild(virtualUnit);
//                                virtualUnit.setPermissionScope(parent);
//                                logger.error("vu " +  parent.getIdentifier());
//                                virtualPermissionScope = parent;
//                                break;
//                            }
//                        } else {
//                            logger.error("language of found description is " + lang + " which is not in the descBundle");
//                        }
//                    }
                    
                }
//                logger.error("virtualPermissionScope idPath: "+virtualPermissionScope.idPath() + " "+ virtualUnit.getPermissionScope().idPath());

            } else{
                logger.error("idPath isEmpty " + idPath.isEmpty() + " mutation created " + mutation.created());
            }

//            VirtualUnit parent = framedGraph.frame(virtualCollection.asVertex(), VirtualUnit.class);
//            logger.error("parent: "+ parent.getIdentifier());
//                parent.addChild(virtualUnit);
//                virtualUnit.setPermissionScope(parent);
        } else {
            logger.error("Unknown virtualcollection for virtual unit: {}", virtualUnit.getIdentifier());
        }
        
        
        

        
        //Try to resolve the undetermined relationships
        //we can only create the annotations after the DocumentaryUnit and its Description have been added to the graph,
        //so they have id's. 
        for (Description unitdesc : unit.getDescriptions()) {
            // Put the set of relationships into a HashSet to remove duplicates.
            for (UndeterminedRelationship rel : Sets.newHashSet(unitdesc.getUndeterminedRelationships())) {
                /*
                 * the wp2 undetermined relationship that can be resolved have a 'cvoc' and a 'concept' attribute.
                 * they need to be found in the vocabularies that are in the graph
                 */
                for (String property : rel.asVertex().getPropertyKeys()) {
                    logger.debug(property);
                }
                if (rel.asVertex().getPropertyKeys().contains("cvoc")) {
                    String cvoc_id = (String) rel.asVertex().getProperty("cvoc");
                    String concept_id = (String) rel.asVertex().getProperty("concept");
                    logger.debug(cvoc_id + "  " + concept_id);
                    Vocabulary vocabulary;
                    try {
                        vocabulary = manager.getFrame(cvoc_id, Vocabulary.class);
                        for (Concept concept : vocabulary.getConcepts()) {
                        logger.debug("*********************" + concept.getId() + " " + concept.getIdentifier());
                        if (concept.getIdentifier().equals(concept_id)) {
                            try {
                                Bundle linkBundle = new Bundle(EntityClass.LINK)
                                        .withDataValue(Ontology.LINK_HAS_TYPE, "resolved relationship")
                                        .withDataValue(Ontology.LINK_HAS_DESCRIPTION, "solved by automatic resolving");
                                Link link = new CrudViews<Link>(framedGraph, Link.class).create(linkBundle, userProfile);
                                virtualUnit.addLink(link);
                                concept.addLink(link);
                                link.addLinkBody(rel);
                            } catch (PermissionDenied ex) {
                                java.util.logging.Logger.getLogger(EadIntoVirtualCollectionImporter.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IntegrityError ex) {
                                java.util.logging.Logger.getLogger(EadIntoVirtualCollectionImporter.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }

                    }
                    } catch (ItemNotFound ex) {
                        logger.error("Vocabulary with id " + cvoc_id +" not found. "+ex.getMessage());
                    }
                    
                }
            }
        }
    }

    void setVirtualCollection(VirtualUnit virtualcollection) {
        this.virtualCollection = virtualcollection;
        this.virtualPermissionScope = virtualcollection;
    }
}
