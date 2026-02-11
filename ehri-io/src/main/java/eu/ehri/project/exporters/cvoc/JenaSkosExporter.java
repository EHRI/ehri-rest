/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.exporters.cvoc;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.cvoc.SkosRDFVocabulary;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.utils.LanguageHelpers;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Export SKOS RDF.
 */
public class JenaSkosExporter implements SkosExporter {

    private static final Logger logger = LoggerFactory.getLogger(JenaSkosExporter.class);
    public final static String DC_URI = "http://purl.org/dc/elements/1.1/";

    private final FramedGraph<?> framedGraph;
    private final Vocabulary vocabulary;
    private final String format;

    public JenaSkosExporter(final FramedGraph<?> framedGraph, final Vocabulary vocabulary, String format) {
        this.framedGraph = framedGraph;
        this.vocabulary = vocabulary;
        this.format = format;
    }

    public JenaSkosExporter(final FramedGraph<?> framedGraph, final Vocabulary vocabulary) {
        this(framedGraph, vocabulary, null);
    }

    public JenaSkosExporter setFormat(String format) {
        return new JenaSkosExporter(framedGraph, vocabulary, format);
    }

    @Override
    public void export(OutputStream outputStream, String base) {
        Model model = export(base);
        RDFWriter writer = model.getWriter(format);
        writer.setProperty("relativeURIs", "");
        writer.write(model, outputStream, base);
    }

    private String getUri(Concept concept, String baseUri) {
        String fallback = baseUri + vocabulary.getIdentifier() + "/" + concept.getIdentifier();
        String orig = concept.getProperty(Ontology.URI_KEY);
        return Optional.ofNullable(orig).orElse(fallback);
    }

    private String getLangCode(Description description) {
        String lang = LanguageHelpers.iso639DashOneCode(description.getLanguageOfDescription());
        String script = description.getProperty(Ontology.IDENTIFIER_KEY);
        return script != null ? lang + "-" + script : lang;
    }

    public Model export(String base) {
        String baseUri = base == null ? SkosRDFVocabulary.DEFAULT_BASE_URI : base;
        Iterable<Concept> concepts = vocabulary.getConcepts();
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(SkosRDFVocabulary.NAMESPACES);

        Resource vocabResource = model.createResource(baseUri + vocabulary.getIdentifier());
        vocabResource.addProperty(RDF.type, SKOS.ConceptScheme);

        // Write name and description as DC elements.
        for (Map.Entry<String, URI> dcElement : SkosRDFVocabulary.VOCAB_PROPS.entrySet()) {
            Object prop = vocabulary.getProperty(dcElement.getKey());
            if (prop != null) {
                writeListOrScalar(model, vocabResource,
                        model.createProperty(dcElement.getValue().toString()), prop, "en");
            }
        }

        for (Concept concept : concepts) {
            Vertex cv = concept.asVertex();
            Resource resource = model.createResource(getUri(concept, baseUri));
            resource.addProperty(RDF.type, SKOS.Concept);
            resource.addProperty(SKOS.inScheme, vocabResource);

            for (String key : cv.getPropertyKeys()) {
                writeProperty(model, resource, key, cv.getProperty(key), null);
            }

            for (Description description : concept.getDescriptions()) {
                Vertex cdv = description.asVertex();
                String lang = getLangCode(description);
                resource.addProperty(SKOS.prefLabel, description.getName(), lang);
                for (String key : cdv.getPropertyKeys()) {
                    writeProperty(model, resource, key, cdv.getProperty(key), lang);
                }

                // In some cases there'll be an unknown property with a key
                // such as owl:sameAs and a value pointing to some other URL.
                for (UnknownProperty prop : description.getUnknownProperties()) {
                    for (String key : prop.getPropertyKeys()) {
                        String value = prop.getProperty(key);
                        if (SkosRDFVocabulary.RELATION_PROPS.containsKey(key)) {
                            resource.addProperty(
                                    model.createProperty(SkosRDFVocabulary.RELATION_PROPS.get(key).toString()), value);
                        }
                    }
                }
            }

            // if there are no broader concepts, assume it's a top concept
            if (!concept.getBroaderConcepts().iterator().hasNext()) {
                vocabResource.addProperty(SKOS.hasTopConcept, resource);
                resource.addProperty(SKOS.topConceptOf, vocabResource);
            }

            for (Concept other : concept.getBroaderConcepts()) {
                Resource otherResource = model.createResource(getUri(other, baseUri));
                resource.addProperty(SKOS.broader, otherResource);
            }
            for (Concept other : concept.getNarrowerConcepts()) {
                Resource otherResource = model.createResource(getUri(other, baseUri));
                resource.addProperty(SKOS.narrower, otherResource);
            }
            for (Concept other : concept.getRelatedConcepts()) {
                Resource otherResource = model.createResource(getUri(other, baseUri));
                resource.addProperty(SKOS.related, otherResource);
            }
        }
        return model;
    }

    private void writeProperty(Model model, Resource resource, String key, Object property, String lang) {
        if (SkosRDFVocabulary.LANGUAGE_PROPS.containsKey(key)) {
            // We map certain URIs to the same internal property, e.g. scopeNote/comment.
            // Ignore all but the first URI when exporting since they're assumed to
            // be in order of preference.
            for (URI uri : SkosRDFVocabulary.LANGUAGE_PROPS.get(key).subList(0, 1)) {
                writeListOrScalar(model, resource, model.createProperty(uri.toString()), property, lang);
            }
        } else if (SkosRDFVocabulary.GENERAL_PROPS.containsKey(key)) {
            writeListOrScalar(model, resource,
                    model.createProperty(SkosRDFVocabulary.GENERAL_PROPS.get(key).toString()), property, null);
        } else if (SkosRDFVocabulary.GEO_PROPS.containsKey(key)) {
            writeListOrScalar(model, resource,
                    model.createProperty(SkosRDFVocabulary.GEO_PROPS.get(key).toString()), property, null);
        }
    }

    private void writeListOrScalar(Model model, Resource resource,
            Property property, Object listOrScalar, String lang) {
        if (listOrScalar instanceof List) {
            List<?> list = (List<?>) listOrScalar;
            for (Object obj : list) {
                logger.trace("Writing list property: {} -> {}", property, obj);
                writeObject(model, resource, property, obj, lang);
            }
        } else {
            logger.trace("Writing scalar property: {} -> {}", property, listOrScalar);
            writeObject(model, resource, property, listOrScalar, lang);
        }
    }

    private void writeObject(Model model, Resource resource, Property property, Object value, String lang) {
        if (value instanceof String) {
            resource.addProperty(property, (String) value, lang);
        } else {
            resource.addProperty(property, model.createTypedLiteral(value));
        }
    }
}
