package eu.ehri.project.importers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
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
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SkosVocabularyImporter {
    private static final Logger logger = LoggerFactory
            .getLogger(SkosVocabularyImporter.class);

    private final FramedGraph<? extends TransactionalGraph> framedGraph;
    private final Actioner actioner;
    private final Vocabulary vocabulary;
    private final BundleDAO dao;
    private final OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory factory = owlManager.getOWLDataFactory();

    public static final String DEFAULT_LANG = "eng";

    // Borrowed from https://github.com/simonjupp/java-skos-api
    public static enum RDFVocabulary {
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

        private String namespace = "http://www.w3.org/2004/02/skos/core#";
        private URI uri;

        RDFVocabulary(String localName) {
            this.uri = URI.create(namespace + localName);
        }

        public URI getURI() {
            return uri;
        }
    }

    public SkosVocabularyImporter(FramedGraph<? extends TransactionalGraph> framedGraph, Actioner actioner,
            Vocabulary vocabulary) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
        this.dao = new BundleDAO(framedGraph, vocabulary.idPath());
    }

    public ImportLog importFile(File dataFile, String logMessage) throws Exception {
        try {
            // Create a new action for this import
            final ActionManager.EventContext eventContext = new ActionManager(framedGraph, vocabulary).logEvent(
                    actioner, EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(eventContext);

            OWLOntology ontology = owlManager.loadOntologyFromOntologyDocument(dataFile);
            OWLClass conceptClass = factory.getOWLClass(IRI.create(RDFVocabulary.CONCEPT.getURI()));

            Map<IRI, Concept> imported = Maps.newHashMap();

            for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(conceptClass)) {
                OWLNamedIndividual item = ax.getIndividual().asOWLNamedIndividual();
                Mutation<Concept> graphConcept = importConcept(item, ontology);
                imported.put(item.getIRI(), graphConcept.getNode());

                switch (graphConcept.getState()) {
                    case UNCHANGED:
                        log.addUnchanged();
                        break;
                    case CREATED:
                        log.addCreated();
                        break;
                    case UPDATED:
                        log.addUpdated();
                        break;
                }
            }

            for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(conceptClass)) {
                hookupRelationships(ax.getIndividual().asOWLNamedIndividual(),
                        ontology, imported);
            }

            for (Concept concept : imported.values()) {
                vocabulary.addConcept(concept);
            }

            if (log.hasDoneWork()) {
                framedGraph.getBaseGraph().commit();
            } else {
                framedGraph.getBaseGraph().rollback();
            }

            return log;
        } catch (ValidationError error) {
            framedGraph.getBaseGraph().rollback();
            throw error;
        }
    }

    private Mutation<Concept> importConcept(OWLNamedIndividual item, OWLOntology dataset) throws ValidationError {
        logger.debug("Importing: {}", item.toString());
        Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT)
                .addDataValue(Ontology.IDENTIFIER_KEY, getId(item.getIRI().toURI()));

        for (Bundle description : getDescriptions(item, dataset)) {
            builder.addRelation(Ontology.DESCRIPTION_FOR_ENTITY, description);
        }

        return dao.createOrUpdate(builder.build(), Concept.class);
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
                IRI value = (IRI)rel.getValue();
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

        connectRelation(current, item, dataset, conceptMap, RDFVocabulary.BROADER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                related.addNarrowerConcept(current);
            }
        });

        connectRelation(current, item, dataset, conceptMap, RDFVocabulary.NARROWER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addNarrowerConcept(related);
            }
        });

        connectRelation(current, item, dataset, conceptMap, RDFVocabulary.RELATED.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addRelatedConcept(related);
            }
        });
    }

    private List<Bundle> getDescriptions(OWLNamedIndividual item, OWLOntology ontology) {

        Map<URI, String> props = ImmutableMap.<URI,String>builder()
               .put(RDFVocabulary.ALT_LABEL.getURI(), Ontology.CONCEPT_ALTLABEL)
               .put(RDFVocabulary.HIDDEN_LABEL.getURI(), Ontology.CONCEPT_HIDDENLABEL)
               .put(RDFVocabulary.DEFINITION.getURI(), Ontology.CONCEPT_DEFINITION)
               .put(RDFVocabulary.SCOPE_NOTE.getURI(), Ontology.CONCEPT_SCOPENOTE)
               .put(RDFVocabulary.NOTE.getURI(), Ontology.CONCEPT_NOTE)
               .put(RDFVocabulary.EDITORIAL_NOTE.getURI(), Ontology.CONCEPT_EDITORIAL_NOTE)
               .build();

        List<Bundle> descriptions = Lists.newArrayList();

        Set<OWLAnnotation> annotations = item.getAnnotations(ontology);

        OWLAnnotationProperty prelLabelProp = factory
                .getOWLAnnotationProperty(IRI.create(RDFVocabulary.PREF_LABEL.getURI()));

        for (OWLAnnotation property : item.getAnnotations(ontology, prelLabelProp)) {
            Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            OWLLiteral literalPrefName = (OWLLiteral)property.getValue();
            String languageCode = literalPrefName.hasLang()
                    ? Helpers.iso639DashTwoCode(literalPrefName.getLang())
                    : DEFAULT_LANG;

            builder.addDataValue(Ontology.NAME_KEY, literalPrefName.getLiteral())
                    .addDataValue(Ontology.LANGUAGE, languageCode);

            for (Map.Entry<URI, String> prop : props.entrySet()) {
                List<String> values = Lists.newArrayList();

                OWLAnnotationProperty annotationProperty = factory
                        .getOWLAnnotationProperty(IRI.create(prop.getKey()));

                for (OWLAnnotation propVal : annotations) {
                    if (propVal.getProperty().equals(annotationProperty)) {
                        OWLLiteral literalProp = (OWLLiteral)propVal.getValue();
                        String propLanguageCode = literalProp.hasLang()
                                ? Helpers.iso639DashTwoCode(literalProp.getLang())
                                : DEFAULT_LANG;
                        if (propLanguageCode.equals(languageCode)) {
                            values.add(literalProp.getLiteral());
                        }
                    }
                }
                if (!values.isEmpty()) {
                    builder.addDataValue(prop.getValue(), values);
                }
            }
            Bundle bundle = builder.build();
            System.out.println(bundle.toJson());
            descriptions.add(bundle);
        }

        return descriptions;
    }

    private String getId(URI uri) {
        return uri.getPath()
                + uri.getQuery()
                + (uri.getFragment() != null ? uri.getFragment() : "");
    }

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
