package eu.ehri.project.importers.cvoc;

import java.net.URI;

/**
* @author Mike Bryant (http://github.com/mikesname)
*/ // Borrowed from https://github.com/simonjupp/java-skos-api
public enum SkosRDFVocabulary {
    LABEL_RELATED("labelRelated"),
    MEMBER("member"),
    MEMBER_LIST("memberList"),
    MAPPING_RELATION("mappingRelation"),
    BROAD_MATCH("broadMatch"),
    NARROW_MATCH("narrowMatch"),
    RELATED_MATCH("relatedMatch"),
    EXACT_MATCH("exactMatch"),
    BROADER("broader"),
    NARROWER("narrower"),
    BROADER_TRANS("broaderTransitive"),
    NARROWER_TRANS("narrowerTransitive"),
    RELATED("related"),
    HAS_TOP_CONCEPT("hasTopConcept"),
    SEMANTIC_RELATION("semanticRelation"),
    CONCEPT("Concept"),
    LABEL_RELATION("LabelRelation"),
    SEE_LABEL_RELATION("seeLabelRelation"),
    COLLECTION("Collection"),
    CONCEPT_SCHEME("ConceptScheme"),
    TOP_CONCEPT_OF("topConceptOf"),
    IN_SCHEME("inScheme"),
    CLOSE_MATCH("closeMatch"),
    DOCUMENT("Document"),
    IMAGE("Image"),
    ORDERED_COLLECTION("OrderedCollection"),
    COLLECTABLE_PROPERTY("CollectableProperty"),
    RESOURCE("Resource"),
    PREF_LABEL("prefLabel"),
    ALT_LABEL("altLabel"),
    COMMENT("comment"),
    EXAMPLE("example"),
    NOTE("note"),
    NOTATION("notation"),
    SCOPE_NOTE("scopeNote"),
    HIDDEN_LABEL("hiddenLabel"),
    EDITORIAL_NOTE("editorialNote"),
    HISTORY_NOTE("historyNote"),
    DEFINITION("definition"),
    CHANGE_NOTE("changeNote");

    private String NAMESPACE = "http://www.w3.org/2004/02/skos/core#";
    private final URI uri;

    SkosRDFVocabulary(String localName) {
        this.uri = URI.create(NAMESPACE + localName);
    }

    public URI getURI() {
        return uri;
    }
}
