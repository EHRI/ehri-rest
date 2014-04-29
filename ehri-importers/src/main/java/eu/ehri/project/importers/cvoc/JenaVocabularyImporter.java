package eu.ehri.project.importers.cvoc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.util.iterator.Map1;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JenaVocabularyImporter {
    private static final Logger logger = LoggerFactory
            .getLogger(JenaVocabularyImporter.class);

    private final FramedGraph<? extends TransactionalGraph> framedGraph;
    private final Actioner actioner;
    private final Vocabulary vocabulary;
    private final BundleDAO dao;

    private boolean tolerant = false;

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

        private static String NAMESPACE = "http://www.w3.org/2004/02/skos/core#";
        private final String localName;

        RDFVocabulary(String localName) {
            this.localName = localName;
        }

        public String getURI() {
            return NAMESPACE + localName;
        }
    }

    public JenaVocabularyImporter(FramedGraph<? extends TransactionalGraph> framedGraph, Actioner actioner,
            Vocabulary vocabulary) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
        this.dao = new BundleDAO(framedGraph, vocabulary.idPath());
    }

    public void setTolerant(boolean tolerant) {
        logger.debug("Setting importer to tolerant: " + tolerant);
        this.tolerant = tolerant;
    }

    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, ValidationError {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError {
        try {
            // Create a new action for this import
            final ActionManager.EventContext eventContext = new ActionManager(framedGraph, vocabulary).logEvent(
                    actioner, EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(eventContext);

            OntModel model = ModelFactory.createOntologyModel();
            model.read(ios, null);
            OntClass conceptClass = model.getOntClass(RDFVocabulary.CONCEPT.getURI());
            ExtendedIterator<? extends OntResource> extendedIterator = conceptClass.listInstances();
            Map<Resource, Concept> imported = Maps.newHashMap();

            while (extendedIterator.hasNext()) {
                Resource item = extendedIterator.next();

                try {
                    Mutation<Concept> graphConcept = importConcept(item);
                    imported.put(item, graphConcept.getNode());

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



            for (Map.Entry<Resource, Concept> pair : imported.entrySet()) {
                hookupRelationships(pair.getKey(), pair.getValue(), imported);
            }

            for (Concept concept : imported.values()) {
                vocabulary.addConcept(concept);
                concept.setPermissionScope(vocabulary);
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

    private Mutation<Concept> importConcept(Resource item) throws ValidationError {
        logger.debug("Importing: {}", item.toString());
        Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT)
                .addDataValue(Ontology.IDENTIFIER_KEY, getId(URI.create(item.getURI())));

        List<Bundle> undetermined = getUndeterminedRelations(item);
        for (Bundle description : getDescriptions(item)) {
            Bundle withRels = description
                    .withRelations(Ontology.HAS_ACCESS_POINT, undetermined);
            builder.addRelation(Ontology.DESCRIPTION_FOR_ENTITY, withRels);
        }

        return dao.createOrUpdate(builder.build(), Concept.class);
    }

    private List<Bundle> getUndeterminedRelations(Resource item) {
        List<Bundle> undetermined = Lists.newArrayList();

        Map<String, String> relations = ImmutableMap.of(
                "owl:sameAs", "http://www.w3.org/2002/07/owl#sameAs"
        );

        for (Map.Entry<String,String> rel : relations.entrySet()) {
            for (RDFNode annotation : getObjectWithPredicate(item, rel.getValue())) {
                if (annotation.isLiteral()) {
                    undetermined.add(new Bundle(EntityClass.UNDETERMINED_RELATIONSHIP)
                    .withDataValue(Ontology.ANNOTATION_TYPE, rel.getKey())
                    .withDataValue(Ontology.NAME_KEY, annotation.toString()));
                }
            }
        }

        return undetermined;
    }

    private static interface ConnectFunc {
        public void connect(Concept current, Concept related);
    }

    public List<RDFNode> getObjectWithPredicate(final Resource item, final String propUri) {
        return item.listProperties().filterKeep(new Filter<Statement>() {
            @Override
            public boolean accept(Statement statement) {
                return statement.getPredicate().hasURI(propUri);
            }
        }).mapWith(new Map1<Statement, RDFNode>() {
            @Override
            public RDFNode map1(Statement statement) {
                return statement.getObject();
            }
        }).toList();
    }

    private void connectRelation(Concept current, Resource item, Map<Resource, Concept> others,
            String propUri, ConnectFunc connectFunc) {
        for (RDFNode other : getObjectWithPredicate(item, propUri)) {
            if (other.isResource()) {
                Concept related = others.get(other.asResource());
                if (related != null) {
                    connectFunc.connect(current, related);
                }
            }
        }
    }

    private void hookupRelationships(Resource item, Concept current,
            Map<Resource, Concept> conceptMap) {

        connectRelation(current, item, conceptMap, RDFVocabulary.BROADER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                related.addNarrowerConcept(current);
            }
        });

        connectRelation(current, item, conceptMap, RDFVocabulary.NARROWER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addNarrowerConcept(related);
            }
        });

        connectRelation(current, item, conceptMap, RDFVocabulary.RELATED.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addRelatedConcept(related);
            }
        });
    }

    private List<Bundle> getDescriptions(Resource item) {

        Map<String, String> props = ImmutableMap.<String,String>builder()
               .put(Ontology.CONCEPT_ALTLABEL, RDFVocabulary.ALT_LABEL.getURI())
               .put(Ontology.CONCEPT_HIDDENLABEL, RDFVocabulary.HIDDEN_LABEL.getURI())
               .put(Ontology.CONCEPT_DEFINITION, RDFVocabulary.DEFINITION.getURI())
               .put(Ontology.CONCEPT_SCOPENOTE, RDFVocabulary.SCOPE_NOTE.getURI())
               .put(Ontology.CONCEPT_NOTE, RDFVocabulary.NOTE.getURI())
               .put(Ontology.CONCEPT_EDITORIAL_NOTE, RDFVocabulary.EDITORIAL_NOTE.getURI())
                .build();
        // Language-agnostic properties.
        Map<String,String> addProps = ImmutableMap.<String,String>builder()
               .put("latitude", "http://www.w3.org/2003/01/geo/wgs84_pos#lat")
               .put("longitude", "http://www.w3.org/2003/01/geo/wgs84_pos#long")
               .build();

        List<Bundle> descriptions = Lists.newArrayList();

        for (RDFNode property : getObjectWithPredicate(item, RDFVocabulary.PREF_LABEL.getURI())) {
            if (!property.isLiteral()) {
                continue;
            }

            Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            Literal literalPrefName = property.asLiteral();
            String languageCode = (literalPrefName.getLanguage() != null)
                    ? Helpers.iso639DashTwoCode(literalPrefName.getLanguage())
                    : DEFAULT_LANG;

            builder.addDataValue(Ontology.NAME_KEY, literalPrefName.getString())
                    .addDataValue(Ontology.LANGUAGE, languageCode);

            for (Map.Entry<String, String> prop: addProps.entrySet()) {
                for (RDFNode annotation : getObjectWithPredicate(item, prop.getValue())) {
                    if (annotation.isLiteral()) {
                        builder.addDataValue(prop.getKey(), annotation.asLiteral().getString());
                    }
                }
            }

            for (Map.Entry<String, String> prop : props.entrySet()) {
                List<String> values = Lists.newArrayList();

                for (RDFNode propVal : getObjectWithPredicate(item, prop.getValue())) {
                    if (propVal.isLiteral()) {
                        Literal literal = propVal.asLiteral();
                        String propLanguageCode = (literal.getLanguage() != null)
                                ? Helpers.iso639DashTwoCode(literal.getLanguage())
                                : DEFAULT_LANG;
                        if (propLanguageCode.equals(languageCode)) {
                            values.add(literal.getString());
                        }
                    }
                }
                if (!values.isEmpty()) {
                    builder.addDataValue(prop.getKey(), values);
                }
            }
            Bundle bundle = builder.build();
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
