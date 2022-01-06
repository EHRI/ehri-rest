/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.SkosMultilingual;
import org.apache.jena.vocabulary.RDFS;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
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

    public static final String DEFAULT_BASE_URI = "http://data.ehri-project.eu/";
    public static final String NAMESPACE_URI = "http://www.w3.org/2004/02/skos/core#";

    public static final Map<String, String> NAMESPACES = ImmutableMap.<String, String>builder()
            .put("skos", "http://www.w3.org/2004/02/skos/core#")
            .put("dc", "http://purl.org/dc/elements/1.1/")
            .put("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
            .put("foaf", "http://xmlns.com/foaf/0.1/")
            .put("sem", "http://semanticweb.cs.vu.nl/2009/11/sem/")
            .put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
            .put("owl", "http://www.w3.org/2002/07/owl#")
            .build();

    // Language-agnostic properties.
    public static final Map<String, URI> GENERAL_PROPS = ImmutableMap.<String, URI>builder()
            .put("latitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#lat"))
            .put("longitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#long"))
            .put("latitude/longitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#lat_long"))
            .put("url", URI.create("http://xmlns.com/foaf/0.1/isPrimaryTopicOf"))
            .put("date", URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/hasTime"))
            .put("person", URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/hasActor"))
            .put("place", URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/hasPlace"))
            .put("seeAlso", URI.create(RDFS.seeAlso.getURI()))
            .build();

    // Properties that end up as undeterminedRelation nodes.
    public static final Map<String, URI> RELATION_PROPS = ImmutableMap.<String, URI>builder()
            .put("owl:sameAs", URI.create("http://www.w3.org/2002/07/owl#sameAs"))
            .put("skos:exactMatch", URI.create("http://www.w3.org/2004/02/skos/core#exactMatch"))
            .put("skos:closeMatch", URI.create("http://www.w3.org/2004/02/skos/core#closeMatch"))
            .put("skos:broadMatch", URI.create("http://www.w3.org/2004/02/skos/core#broadMatch"))
            .put("skos:relatedMatch", URI.create("http://www.w3.org/2004/02/skos/core#relatedMatch"))
            .put("sem:person", URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/hasActor"))
            .put("sem:place", URI.create("http://semanticweb.cs.vu.nl/2009/11/sem/hasPlace"))
            .build();

    public static final Map<String, List<URI>> LANGUAGE_PROPS = ImmutableMap.<String, List<URI>>builder()
            .put(SkosMultilingual.altLabel.name(), Lists.newArrayList(ALT_LABEL.getURI()))
            .put(SkosMultilingual.hiddenLabel.name(), Lists.newArrayList(HIDDEN_LABEL.getURI()))
            .put(SkosMultilingual.definition.name(), Lists.newArrayList(DEFINITION.getURI()))
            .put(SkosMultilingual.note.name(), Lists.newArrayList(NOTE.getURI()))
            .put(SkosMultilingual.changeNote.name(), Lists.newArrayList(CHANGE_NOTE.getURI()))
            .put(SkosMultilingual.editorialNote.name(), Lists.newArrayList(EDITORIAL_NOTE.getURI()))
            .put(SkosMultilingual.historyNote.name(), Lists.newArrayList(HISTORY_NOTE.getURI()))
            .put(SkosMultilingual.scopeNote.name(), Lists.newArrayList(SCOPE_NOTE.getURI(),
                    URI.create("http://www.w3.org/2000/01/rdf-schema#comment")))
            .build();

    private final URI uri;

    SkosRDFVocabulary(String localName) {
        this.uri = URI.create(NAMESPACE_URI + localName);
    }

    public URI getURI() {
        return uri;
    }
}
