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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class OwlApiSkosImporter implements SkosImporter {
    private static final Logger logger = LoggerFactory
            .getLogger(OwlApiSkosImporter.class);

    private final FramedGraph<?> framedGraph;
    private final Actioner actioner;
    private final Vocabulary vocabulary;
    private final BundleDAO dao;
    private final OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory factory = owlManager.getOWLDataFactory();

    private final boolean tolerant;
    private final String format;
    private final String defaultLang;

    public static final String DEFAULT_LANG = "eng";

    public OwlApiSkosImporter(FramedGraph<?> framedGraph, Actioner actioner,
            Vocabulary vocabulary, boolean tolerant, String format, String defaultLang) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
        this.tolerant = tolerant;
        this.format = format;
        this.defaultLang = defaultLang;
        this.dao = new BundleDAO(framedGraph, vocabulary.idPath());
    }

    public OwlApiSkosImporter(FramedGraph<?> framedGraph, Actioner actioner,
            Vocabulary vocabulary) {
        this(framedGraph, actioner, vocabulary, false, null, DEFAULT_LANG);
    }

    public OwlApiSkosImporter setTolerant(boolean tolerant) {
        logger.debug("Setting importer to tolerant: " + tolerant);
        return new OwlApiSkosImporter(framedGraph, actioner, vocabulary, tolerant, format, defaultLang);
    }

    public OwlApiSkosImporter setFormat(String format) {
        logger.warn("Setting format on " + getClass().getSimpleName() + " is currently ignored");
        return this;
    }

    public OwlApiSkosImporter setDefaultLang(String lang) {
        return new OwlApiSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, format,
                Helpers.iso639DashTwoCode(lang));
    }

    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, InputParseError, ValidationError {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError, InputParseError {

        // Create a new action for this import
        final ActionManager.EventContext eventContext = new ActionManager(framedGraph, vocabulary).newEventContext(
                actioner, EventTypes.ingest, getLogMessage(logMessage));
        // Create a manifest to store the results of the import.
        final ImportLog log = new ImportLog(eventContext);

        OWLOntology ontology;
        try {
            ontology = owlManager.loadOntologyFromOntologyDocument(ios);
        } catch (OWLOntologyCreationException e) {
            throw new InputParseError(e);
        }

        OWLClass conceptClass = factory.getOWLClass(IRI.create(SkosRDFVocabulary.CONCEPT.getURI()));

        Map<IRI, Concept> imported = Maps.newHashMap();

        for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(conceptClass)) {
            OWLNamedIndividual item = ax.getIndividual().asOWLNamedIndividual();
            try {
                Mutation<Concept> graphConcept = importConcept(item, ontology);
                imported.put(item.getIRI(), graphConcept.getNode());

                switch (graphConcept.getState()) {
                    case UNCHANGED:
                        log.addUnchanged();
                        break;
                    case CREATED:
                        log.addCreated();
                        eventContext.addSubjects(graphConcept.getNode());
                        break;
                    case UPDATED:
                        log.addUpdated();
                        eventContext.addSubjects(graphConcept.getNode());
                        break;
                }
            } catch (ValidationError validationError) {
                if (tolerant) {
                    logger.error(validationError.getMessage());
                    log.setErrored(item.toString(), validationError.getMessage());
                } else {
                    throw validationError;
                }
            }
        }

        for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(conceptClass)) {
            hookupRelationships(ax.getIndividual().asOWLNamedIndividual(),
                    ontology, imported);
        }

        for (Concept concept : imported.values()) {
            vocabulary.addItem(concept);
            concept.setPermissionScope(vocabulary);
        }

        if (log.hasDoneWork()) {
            eventContext.commit();
        }

        return log;
    }

    private Mutation<Concept> importConcept(OWLNamedIndividual item, OWLOntology dataset) throws ValidationError {
        logger.debug("Importing: {}", item.toString());
        Bundle.Builder builder = Bundle.Builder.withClass(EntityClass.CVOC_CONCEPT)
                .addDataValue(Ontology.IDENTIFIER_KEY, getId(item.getIRI().toURI()));

        List<Bundle> undetermined = getUndeterminedRelations(item, dataset);
        for (Bundle description : getDescriptions(item, dataset)) {
            Bundle withRels = description
                    .withRelations(Ontology.HAS_ACCESS_POINT, undetermined);
            builder.addRelation(Ontology.DESCRIPTION_FOR_ENTITY, withRels);
        }

        return dao.createOrUpdate(builder.build(), Concept.class);
    }

    private List<Bundle> getUndeterminedRelations(OWLNamedIndividual item, OWLOntology dataset) {
        List<Bundle> undetermined = Lists.newArrayList();

        Map<String, IRI> rels = ImmutableMap.of(
                "owl:sameAs", IRI.create("http://www.w3.org/2002/07/owl#sameAs")
        );

        for (Map.Entry<String, IRI> rel : rels.entrySet()) {
            OWLAnnotationProperty propName = factory.getOWLAnnotationProperty(
                    rel.getValue());
            Set<OWLAnnotation> annotations = item.getAnnotations(dataset, propName);
            for (OWLAnnotation annotation : annotations) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    undetermined.add(new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP)
                            .withDataValue(Ontology.ANNOTATION_TYPE, rel.getKey())
                            .withDataValue(Ontology.NAME_KEY, rel.getValue().toString()));
                }
            }
        }

        return undetermined;
    }

    private static interface ConnectFunc {
        public void connect(Concept current, Concept related);
    }

    private void connectRelation(Concept current, OWLNamedIndividual item,
            OWLOntology ontology, Map<IRI, Concept> others,
            URI propUri, ConnectFunc connectFunc) {
        OWLAnnotationProperty property = factory.getOWLAnnotationProperty(
                IRI.create(propUri));
        for (OWLAnnotation rel : item.getAnnotations(ontology, property)) {
            // If it's not an IRI we can't do much here.
            if (rel.getValue() instanceof IRI) {
                IRI value = (IRI) rel.getValue();
                Concept related = others.get(value);
                if (related != null) {
                    connectFunc.connect(current, related);
                }
            }
        }
    }

    private void hookupRelationships(OWLNamedIndividual item, OWLOntology dataset,
            Map<IRI, Concept> conceptMap) {
        Concept current = conceptMap.get(item.getIRI());

        connectRelation(current, item, dataset, conceptMap, SkosRDFVocabulary.BROADER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                related.addNarrowerConcept(current);
            }
        });

        connectRelation(current, item, dataset, conceptMap, SkosRDFVocabulary.NARROWER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addNarrowerConcept(related);
            }
        });

        connectRelation(current, item, dataset, conceptMap, SkosRDFVocabulary.RELATED.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addRelatedConcept(related);
            }
        });
    }

    private List<Bundle> getDescriptions(OWLNamedIndividual item, OWLOntology ontology) {

        Map<String, URI> props = ImmutableMap.<String, URI>builder()
                .put(Ontology.CONCEPT_ALTLABEL, SkosRDFVocabulary.ALT_LABEL.getURI())
                .put(Ontology.CONCEPT_HIDDENLABEL, SkosRDFVocabulary.HIDDEN_LABEL.getURI())
                .put(Ontology.CONCEPT_DEFINITION, SkosRDFVocabulary.DEFINITION.getURI())
                .put(Ontology.CONCEPT_SCOPENOTE, SkosRDFVocabulary.SCOPE_NOTE.getURI())
                .put(Ontology.CONCEPT_NOTE, SkosRDFVocabulary.NOTE.getURI())
                .put(Ontology.CONCEPT_EDITORIAL_NOTE, SkosRDFVocabulary.EDITORIAL_NOTE.getURI())
                .build();
        // Language-agnostic properties.
        Map<String, URI> addProps = ImmutableMap.<String, URI>builder()
                .put("latitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#lat"))
                .put("longitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#long"))
                .build();

        List<Bundle> descriptions = Lists.newArrayList();

        Set<OWLAnnotation> annotations = item.getAnnotations(ontology);

        OWLAnnotationProperty prelLabelProp = factory
                .getOWLAnnotationProperty(IRI.create(SkosRDFVocabulary.PREF_LABEL.getURI()));

        for (OWLAnnotation property : item.getAnnotations(ontology, prelLabelProp)) {
            Bundle.Builder builder = Bundle.Builder.withClass(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            OWLLiteral literalPrefName = (OWLLiteral) property.getValue();
            String languageCode = literalPrefName.hasLang()
                    ? Helpers.iso639DashTwoCode(literalPrefName.getLang())
                    : defaultLang;

            builder.addDataValue(Ontology.NAME_KEY, literalPrefName.getLiteral())
                    .addDataValue(Ontology.LANGUAGE, languageCode);

            for (Map.Entry<String, URI> prop : addProps.entrySet()) {
                OWLAnnotationProperty annotationProperty = factory
                        .getOWLAnnotationProperty(IRI.create(prop.getValue()));
                for (OWLAnnotation annotation : item.getAnnotations(ontology, annotationProperty)) {
                    OWLLiteral literalProp = (OWLLiteral) annotation.getValue();
                    builder.addDataValue(prop.getKey(), literalProp.getLiteral());
                }
            }

            for (Map.Entry<String, URI> prop : props.entrySet()) {
                List<String> values = Lists.newArrayList();

                OWLAnnotationProperty annotationProperty = factory
                        .getOWLAnnotationProperty(IRI.create(prop.getValue()));

                for (OWLAnnotation propVal : annotations) {
                    if (propVal.getProperty().equals(annotationProperty)) {
                        OWLLiteral literalProp = (OWLLiteral) propVal.getValue();
                        String propLanguageCode = literalProp.hasLang()
                                ? Helpers.iso639DashTwoCode(literalProp.getLang())
                                : defaultLang;
                        if (propLanguageCode.equals(languageCode)) {
                            values.add(literalProp.getLiteral());
                        }
                    }
                }
                if (!values.isEmpty()) {
                    builder.addDataValue(prop.getKey(), values);
                }
            }
            Bundle bundle = builder.build();
            logger.trace(bundle.toJson());
            descriptions.add(bundle);
        }

        return descriptions;
    }

    private String getId(URI uri) {
        return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                + (uri.getQuery() != null ? uri.getQuery() : "")
                + (uri.getFragment() != null ? uri.getFragment() : "");
    }

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
