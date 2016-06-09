package eu.ehri.project.api;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.cvoc.Concept;

import java.util.List;

public interface ConceptsApi {

    Concept addRelatedConcepts(Concept concept, List<String> related) throws ItemNotFound, PermissionDenied;

    Concept removeRelatedConcepts(Concept concept, List<String> related) throws ItemNotFound, PermissionDenied;

    Concept addNarrowerConcepts(Concept concept, List<String> narrower) throws ItemNotFound, PermissionDenied;

    Concept removeNarrowerConcepts(Concept concept, List<String> narrower) throws ItemNotFound, PermissionDenied;
}
