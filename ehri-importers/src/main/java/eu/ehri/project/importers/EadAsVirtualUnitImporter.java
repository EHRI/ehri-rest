/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class EadAsVirtualUnitImporter extends EadImporter{
    private static final Logger logger = LoggerFactory.getLogger(EadAsVirtualUnitImporter.class);
    
    public EadAsVirtualUnitImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
        this.importAsVirtualCollection();
        logger.debug("ead will be imported as VirtualUnits");
    }
}
