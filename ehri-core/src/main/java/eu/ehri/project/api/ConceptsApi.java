package eu.ehri.project.api;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.cvoc.Concept;

import java.util.List;

public interface ConceptsApi {

    Concept addRelatedConcepts(String id, List<String> related)
            throws ItemNotFound, PermissionDenied;

    Concept removeRelatedConcepts(String id, List<String> related)
            throws ItemNotFound, PermissionDenied;

    Concept addNarrowerConcepts(String id, List<String> narrower)
            throws ItemNotFound, PermissionDenied;

    Concept removeNarrowerConcepts(String id, List<String> narrower)
            throws ItemNotFound, PermissionDenied;

    Concept setBroaderConcepts(String id, List<String> broader)
            throws ItemNotFound, PermissionDenied, DeserializationError;
}
