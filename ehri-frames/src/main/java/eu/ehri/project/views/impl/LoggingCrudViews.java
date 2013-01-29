package eu.ehri.project.views.impl;

import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.ActionManager;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.Crud;

/**
 * Views class that handles creating Action objects that provide an audit log
 * for CRUD actions.
 * 
 * @author michaelb
 * 
 * @param <E>
 */
public class LoggingCrudViews<E extends AccessibleEntity> implements Crud<E> {

    // Default log strings, needed for compatibility.
    public static final String DEFAULT_CREATE_LOG = "Creating item";
    public static final String DEFAULT_UPDATE_LOG = "Updating item";
    public static final String DEFAULT_DELETE_LOG = "Deleting item";
    public static final String DEFAULT_IMPORT_LOG = "Importing item";

    private final ActionManager actionManager;
    private final CrudViews<E> views;
    private final FramedGraph<Neo4jGraph> graph;
    private final Class<E> cls;
    @SuppressWarnings("unused")
    private final PermissionScope scope;

    /**
     * Scoped Constructor.
     * 
     * @param graph
     * @param cls
     */
    public LoggingCrudViews(FramedGraph<Neo4jGraph> graph, Class<E> cls,
            PermissionScope scope) {
        this.graph = graph;
        this.cls = cls;
        this.scope = scope;
        actionManager = new ActionManager(graph);
        views = new CrudViews<E>(graph, cls, scope);
    }

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public LoggingCrudViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance());
    }

    /**
     * Create a new object of type `E` from the given data, saving an Action log
     * with the default creation message.
     * 
     * @param data
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public E create(Bundle bundle, Accessor user) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return create(bundle, user, DEFAULT_CREATE_LOG);
    }

    /**
     * Create a new object of type `E` from the given data, saving an Action log
     * with the given log message.
     * 
     * @param data
     * @param user
     * @param logMessage
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throw DeserializationError
     */
    public E create(Bundle bundle, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        // Behold: A compelling reason to upgrade to Java 7
        // http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html
        try {

            E out = views.create(bundle, user);
            actionManager.logEvent(out, user, logMessage);
            tx.success();
            return out;
        } catch (IntegrityError ex) {
            tx.failure();
            throw ex;
        } catch (PermissionDenied ex) {
            tx.failure();
            throw ex;
        } catch (ValidationError ex) {
            tx.failure();
            throw ex;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        } finally {
            tx.finish();
        }
    }

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the default creation message.
     * 
     * @param data
     * @param user
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public E createOrUpdate(Bundle bundle, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        return createOrUpdate(bundle, user, DEFAULT_IMPORT_LOG);
    }

    /**
     * Create or update a new object of type `E` from the given data, saving an
     * Action log with the given log message.
     * 
     * @param data
     * @param user
     * @param logMessage
     * @return The created framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throw DeserializationError
     */
    public E createOrUpdate(Bundle bundle, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {

            E out = views.createOrUpdate(bundle, user);
            actionManager.logEvent(out, user, logMessage);
            tx.success();
            return out;
        } catch (IntegrityError ex) {
            tx.failure();
            throw ex;
        } catch (PermissionDenied ex) {
            tx.failure();
            throw ex;
        } catch (ValidationError ex) {
            tx.failure();
            throw ex;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        } finally {
            tx.finish();
        }
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the default update message.
     * 
     * @param data
     * @param user
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     */
    public E update(Bundle bundle, Accessor user) throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        return update(bundle, user, DEFAULT_UPDATE_LOG);
    }

    /**
     * Update an object of type `E` from the given data, saving an Action log
     * with the given log message.
     * 
     * @param data
     * @param user
     * @param logMessage
     * @return The updated framed vertex
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throw DeserializationError
     */
    public E update(Bundle bundle, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            E out = views.update(bundle, user);
            actionManager.logEvent(out, user, logMessage);
            tx.success();
            return out;
        } catch (IntegrityError ex) {
            tx.failure();
            throw ex;
        } catch (PermissionDenied ex) {
            tx.failure();
            throw ex;
        } catch (ValidationError ex) {
            tx.failure();
            throw ex;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        } finally {
            tx.finish();
        }
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the default deletion message.
     * 
     * @param item
     * @param user
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user) throws PermissionDenied,
            ValidationError, SerializationError {
        return delete(item, user, DEFAULT_DELETE_LOG);
    }

    /**
     * Delete an object bundle, following dependency cascades, saving an Action
     * log with the given deletion message.
     * 
     * @param item
     * @param user
     * @param logMessage
     * @return The number of vertices deleted
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws SerializationError
     */
    public Integer delete(E item, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, SerializationError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            actionManager.logEvent(item, user, logMessage);
            Integer count = views.delete(item, user);
            tx.success();
            return count;
        } catch (PermissionDenied ex) {
            tx.failure();
            throw ex;
        } catch (ValidationError ex) {
            tx.failure();
            throw ex;
        } catch (Exception ex) {
            tx.failure();
            throw new RuntimeException(ex);
        } finally {
            tx.finish();
        }
    }

    public Crud<E> setScope(PermissionScope scope) {
        return new LoggingCrudViews<E>(graph, cls, scope);
    }

    public E detail(E item, Accessor user) throws PermissionDenied {
        return views.detail(item, user);
    }
}
