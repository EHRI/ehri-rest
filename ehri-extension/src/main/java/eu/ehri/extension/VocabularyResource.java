package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a RESTful interface for the Vocabulary Also for managing the
 * Concepts that are in the Vocabulary
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 */
@Path(Entities.CVOC_VOCABULARY)
public class VocabularyResource extends
        AbstractAccessibleEntityResource<Vocabulary> {

    public VocabularyResource(@Context GraphDatabaseService database) {
        super(database, Vocabulary.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getVocabulary(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public long countVocabularies() throws ItemNotFound, BadRequester {
        return count();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listVocabularies() throws ItemNotFound, BadRequester {
        return page();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public long countVocabularyConcepts(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(id, user);
        return getQuery(cls).count(vocabulary.getConcepts());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public Response listVocabularyConcepts(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(id, user);
        return streamingPage(getQuery(Concept.class)
                .page(vocabulary.getConcepts(), user));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createVocabulary(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        return create(bundle, accessors);
    }

    // Note: bundle contains id
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateVocabulary(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateVocabulary(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteVocabulary(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /**
     * Concept manipulation **
     */

    @DELETE
    @Path("/{id:.+}/all")
    public Response deleteAllVocabularyConcepts(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied, PermissionDenied {
        try {
            UserProfile user = getCurrentUser();
            Vocabulary vocabulary = views.detail(id, user);
            CrudViews<Concept> conceptViews = new CrudViews<Concept>(
                    graph, Concept.class, vocabulary);
            ActionManager actionManager = new ActionManager(graph, vocabulary);
            Iterable<Concept> concepts = vocabulary.getConcepts();
            if (concepts.iterator().hasNext()) {
                ActionManager.EventContext context = actionManager
                        .logEvent(user, EventTypes.deletion, getLogMessage());
                for (Concept concept : concepts) {
                    context.addSubjects(concept);
                    conceptViews.delete(concept.getId(), user);
                }
            }
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (ValidationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create a top-level concept unit for this vocabulary.
     *
     * @param id     The vocabulary ID
     * @param bundle The new concept data
     * @return The new concept
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.CVOC_CONCEPT)
    public Response createVocabularyConcept(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        try {
            final Accessor user = getRequesterUserProfile();
            final Vocabulary vocabulary = views.detail(id, user);
            return create(bundle, accessors, new Handler<Concept>() {
                @Override
                public void process(Concept concept) throws PermissionDenied {
                    concept.setVocabulary(vocabulary);
                }
            }, views.setScope(vocabulary).setClass(Concept.class));
        } finally {
            cleanupTransaction();
        }
    }
}
