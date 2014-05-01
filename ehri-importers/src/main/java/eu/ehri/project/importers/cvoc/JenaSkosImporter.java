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
public class JenaSkosImporter implements SkosImporter {
    private static final Logger logger = LoggerFactory
            .getLogger(JenaSkosImporter.class);

    private final FramedGraph<? extends TransactionalGraph> framedGraph;
    private final Actioner actioner;
    private final Vocabulary vocabulary;
    private final BundleDAO dao;

    private boolean tolerant = false;
    private String format = null;

    public static final String DEFAULT_LANG = "eng";

    // Language-sensitive properties.
    public static final Map<String, URI> LANGUAGE_PROPS = ImmutableMap.<String, URI>builder()
            .put(Ontology.CONCEPT_ALTLABEL, SkosRDFVocabulary.ALT_LABEL.getURI())
            .put(Ontology.CONCEPT_HIDDENLABEL, SkosRDFVocabulary.HIDDEN_LABEL.getURI())
            .put(Ontology.CONCEPT_DEFINITION, SkosRDFVocabulary.DEFINITION.getURI())
            .put(Ontology.CONCEPT_SCOPENOTE, SkosRDFVocabulary.SCOPE_NOTE.getURI())
            .put(Ontology.CONCEPT_NOTE, SkosRDFVocabulary.NOTE.getURI())
            .put(Ontology.CONCEPT_EDITORIAL_NOTE, SkosRDFVocabulary.EDITORIAL_NOTE.getURI())
            .build();

    // Language-agnostic properties.
    public static final Map<String, URI> GENERAL_PROPS = ImmutableMap.<String, URI>builder()
            .put("latitude",URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#lat"))
            .put("longitude", URI.create("http://www.w3.org/2003/01/geo/wgs84_pos#long"))
            .build();

    // Properties that end up as undeterminedRelation nodes.
    public static final Map<String, URI> RELATION_PROPS = ImmutableMap.of(
            "owl:sameAs", URI.create("http://www.w3.org/2002/07/owl#sameAs")
    );

    /**
     * Constructor
     *
     * @param framedGraph The framed graph
     * @param actioner    The actioner
     * @param vocabulary  The target vocabulary
     */
    public JenaSkosImporter(FramedGraph<? extends TransactionalGraph> framedGraph, Actioner actioner,
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

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Import a file by path.
     *
     * @param filePath   The SKOS file path
     * @param logMessage A log message
     * @return A log of imported nodes
     * @throws IOException
     * @throws ValidationError
     */
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, ValidationError {
        FileInputStream ios = new FileInputStream(filePath);
        try {
            return importFile(ios, logMessage);
        } finally {
            ios.close();
        }
    }

    /**
     * Import an input stream.
     *
     * @param ios        The SKOS file input stream
     * @param logMessage A log message
     * @return A log of imported nodes
     * @throws IOException
     * @throws ValidationError
     */
    public ImportLog importFile(InputStream ios, String logMessage)
            throws IOException, ValidationError {
        try {
            // Create a new action for this import
            final ActionManager.EventContext eventContext = new ActionManager(framedGraph, vocabulary).logEvent(
                    actioner, EventTypes.ingest, getLogMessage(logMessage));
            // Create a manifest to store the results of the import.
            final ImportLog log = new ImportLog(eventContext);

            OntModel model = ModelFactory.createOntologyModel();
            model.read(ios, null, format);
            OntClass conceptClass = model.getOntClass(SkosRDFVocabulary.CONCEPT.getURI().toString());
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
                vocabulary.addItem(concept);
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

        for (Map.Entry<String, URI> rel : RELATION_PROPS.entrySet()) {
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

    private List<RDFNode> getObjectWithPredicate(final Resource item, final URI propUri) {
        // NB: this should be possible with simply item.listProperties(propUri)
        // but for some reason that doesn't work... I can't grok why.
        return item.listProperties().filterKeep(new Filter<Statement>() {
            @Override
            public boolean accept(Statement statement) {
                return statement.getPredicate().hasURI(propUri.toString());
            }
        }).mapWith(new Map1<Statement, RDFNode>() {
            @Override
            public RDFNode map1(Statement statement) {
                return statement.getObject();
            }
        }).toList();
    }

    private void connectRelation(Concept current, Resource item, Map<Resource, Concept> others,
            URI propUri, ConnectFunc connectFunc) {
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

        connectRelation(current, item, conceptMap, SkosRDFVocabulary.BROADER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                related.addNarrowerConcept(current);
            }
        });

        connectRelation(current, item, conceptMap, SkosRDFVocabulary.NARROWER.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addNarrowerConcept(related);
            }
        });

        connectRelation(current, item, conceptMap, SkosRDFVocabulary.RELATED.getURI(), new ConnectFunc() {
            @Override
            public void connect(Concept current, Concept related) {
                current.addRelatedConcept(related);
            }
        });
    }

    private List<Bundle> getDescriptions(Resource item) {


        List<Bundle> descriptions = Lists.newArrayList();

        for (RDFNode property : getObjectWithPredicate(item, SkosRDFVocabulary.PREF_LABEL.getURI())) {
            if (!property.isLiteral()) {
                continue;
            }

            Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            Literal literalPrefName = property.asLiteral();
            String languageCode = isValidLanguageCode(literalPrefName.getLanguage())
                    ? Helpers.iso639DashTwoCode(literalPrefName.getLanguage())
                    : DEFAULT_LANG;

            builder.addDataValue(Ontology.NAME_KEY, literalPrefName.getString())
                    .addDataValue(Ontology.LANGUAGE, languageCode);

            for (Map.Entry<String, URI> prop : GENERAL_PROPS.entrySet()) {
                for (RDFNode target : getObjectWithPredicate(item, prop.getValue())) {
                    if (target.isLiteral()) {
                        builder.addDataValue(prop.getKey(), target.asLiteral().getString());
                    }
                }
            }

            for (Map.Entry<String, URI> prop : LANGUAGE_PROPS.entrySet()) {
                List<String> values = Lists.newArrayList();

                for (RDFNode target : getObjectWithPredicate(item, prop.getValue())) {
                    if (target.isLiteral()) {
                        Literal literal = target.asLiteral();
                        String propLanguageCode = isValidLanguageCode(literal.getLanguage())
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

    private boolean isValidLanguageCode(String language) {
        return !(language == null || language.isEmpty());
    }
}
