/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.PermissionScope;

/**
 *
 * @author linda
 */
public abstract class CsvImporter<T> extends XmlImporter<T> {
    public CsvImporter(FramedGraph<Neo4jGraph> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }
    public abstract String getProperty(String key);
}
