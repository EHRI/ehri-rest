/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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

    private final String NAMESPACE = "http://www.w3.org/2004/02/skos/core#";
    private final URI uri;

    SkosRDFVocabulary(String localName) {
        this.uri = URI.create(NAMESPACE + localName);
    }

    public URI getURI() {
        return uri;
    }
}
