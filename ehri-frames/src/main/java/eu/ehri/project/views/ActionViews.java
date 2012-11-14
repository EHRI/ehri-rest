package eu.ehri.project.views;

import java.util.Map;

import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.ActionManager;

/**
 * Views class that handles creating Action objects that provide an audit log
 * for CRUD actions.
 * 
 * @author michaelb
 * 
 * @param <E>
 */
public class ActionViews<E extends AccessibleEntity> extends Views<E> implements
        IViews<E> {

    // Default log strings, needed for compatibility.
    public static final String DEFAULT_CREATE_LOG = "Creating item";
    public static final String DEFAULT_UPDATE_LOG = "Updating item";
    public static final String DEFAULT_DELETE_LOG = "Deleting item";

    private ActionManager actionManager;

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public ActionViews(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        super(graph, cls);
        actionManager = new ActionManager(graph);
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
    @Override
    public E create(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        return create(data, user, DEFAULT_CREATE_LOG);
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
    public E create(Map<String, Object> data, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        // Behold: A compelling reason to upgrade to Java 7
        // http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html
        try {

            E out = super.create(data, user);
            actionManager.createAction(out,
                    graph.frame(user.asVertex(), UserProfile.class),
                    logMessage);
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
        } catch (DeserializationError ex) {
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
    @Override
    public E update(Map<String, Object> data, Accessor user)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        return update(data, user, DEFAULT_UPDATE_LOG);
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
    public E update(Map<String, Object> data, Accessor user, String logMessage)
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            E out = super.update(data, user);
            actionManager
                    .createAction(out,
                            graph.frame(user.asVertex(), UserProfile.class),
                            logMessage);
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
        } catch (DeserializationError ex) {
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
    @Override
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
            actionManager
                    .createAction(item,
                            graph.frame(user.asVertex(), UserProfile.class),
                            logMessage);
            Integer count = super.delete(item, user);
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
}
