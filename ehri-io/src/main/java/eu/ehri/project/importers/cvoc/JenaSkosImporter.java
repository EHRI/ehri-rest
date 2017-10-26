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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ApiFactory;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.definitions.SkosMultilingual;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.utils.LanguageHelpers;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Import SKOS RDF.
 */
public final class JenaSkosImporter implements SkosImporter {

    private static final Logger logger = LoggerFactory.getLogger(JenaSkosImporter.class);
    private static final Config config = ConfigFactory.load();
    private static final Splitter codeSplitter = Splitter.on('-')
            .omitEmptyStrings().trimResults().limit(2);
    private final FramedGraph<?> framedGraph;
    private final Actioner actioner;
    private final Vocabulary vocabulary;
    private final BundleManager dao;
    private final Api api;
    private final Serializer mergeSerializer;
    private final boolean tolerant;
    private final String format;
    private final String baseURI;
    private final String suffix;
    private final String defaultLang;
    private static final String DEFAULT_LANG = Locale.ENGLISH.getISO3Language();
    private static final Bundle linkTemplate = Bundle.of(EntityClass.LINK)
            .withDataValue(Ontology.LINK_HAS_DESCRIPTION, config.getString("io.import.defaultLinkText"))
            .withDataValue(Ontology.LINK_HAS_TYPE, config.getString("io.import.defaultLinkType"));

    /**
     * Constructor
     *
     * @param framedGraph The framed graph
     * @param actioner    The actioner
     * @param vocabulary  The target vocabulary
     * @param tolerant    Whether or not to ignore single item validation errors.
     * @param format      The RDF format
     * @param defaultLang The language to use for elements without specified language
     */
    public JenaSkosImporter(FramedGraph<?> framedGraph, Actioner actioner,
            Vocabulary vocabulary, boolean tolerant, String baseURI, String suffix, String format, String defaultLang) {
        this.framedGraph = framedGraph;
        this.actioner = actioner;
        this.vocabulary = vocabulary;
        this.api = ApiFactory.noLogging(framedGraph, actioner.as(Accessor.class));
        this.mergeSerializer = new Serializer.Builder(framedGraph).dependentOnly().build();
        this.tolerant = tolerant;
        this.baseURI = baseURI;
        this.suffix = suffix;
        this.format = format;
        this.defaultLang = defaultLang;
        this.dao = new BundleManager(framedGraph, vocabulary.idPath());
    }

    private static class StringValue {
        private final String str;
        private final String lang;

        StringValue(String str, String lang) {
            this.str = str;
            this.lang = lang;
        }

        String getValue() {
            return str;
        }

        String getLang() {
            return lang;
        }

        @Override
        public String toString() {
            return "\"" + str + "\"" + (
                    lang == null || lang.trim().isEmpty() ? "" : "@" + lang);
        }
    }

    /**
     * Constructor
     *
     * @param framedGraph The framed graph
     * @param actioner    The actioner
     * @param vocabulary  The target vocabulary
     */
    public JenaSkosImporter(FramedGraph<?> framedGraph, Actioner actioner,
            Vocabulary vocabulary) {
        this(framedGraph, actioner, vocabulary, false, null, null, null, DEFAULT_LANG);
    }

    @Override
    public JenaSkosImporter setTolerant(boolean tolerant) {
        logger.debug("Setting importer to tolerant: {}", tolerant);
        return new JenaSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, baseURI, suffix, format, defaultLang);
    }

    @Override
    public JenaSkosImporter setBaseURI(String prefix) {
        logger.debug("Setting importer base URI: {}", prefix);
        return new JenaSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, prefix, suffix, format, defaultLang);
    }

    @Override
    public JenaSkosImporter setURISuffix(String suffix) {
        logger.debug("Setting importer URI: suffix {}", suffix);
        return new JenaSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, baseURI, suffix, format, defaultLang);
    }

    @Override
    public JenaSkosImporter setFormat(String format) {
        logger.debug("Setting importer format: {}", format);
        return new JenaSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, baseURI, suffix, format, defaultLang);
    }

    @Override
    public JenaSkosImporter setDefaultLang(String lang) {
        logger.debug("Setting importer default language: {}", lang);
        return new JenaSkosImporter(
                framedGraph, actioner, vocabulary, tolerant, baseURI, suffix, format,
                LanguageHelpers.iso639DashTwoCode(lang));
    }

    /**
     * Import a file by path.
     *
     * @param filePath   The SKOS file path
     * @param logMessage A log message
     * @return A log of imported nodes
     */
    @Override
    public ImportLog importFile(String filePath, String logMessage)
            throws IOException, ValidationError {
        try (InputStream ios = Files.newInputStream(Paths.get(filePath))) {
            return importFile(ios, logMessage);
        }
    }

    /**
     * Import an input stream.
     *
     * @param ios        The SKOS file input stream
     * @param logMessage A log message
     * @return A log of imported nodes
     */
    @Override
    public ImportLog importFile(InputStream ios, String logMessage) throws IOException, ValidationError {

        // Create a new action for this import
        Optional<String> logMsg = getLogMessage(logMessage);
        ActionManager.EventContext eventContext = new ActionManager(framedGraph, vocabulary).newEventContext(
                actioner, EventTypes.ingest, logMsg);
        // Create a manifest to store the results of the import.
        ImportLog log = new ImportLog(logMsg.orElse(null));

        // NB: We rely in inference here, despite a sketchy understanding of how it works ;)
        // The RDFS.subPropertyOf lets us fetch SKOS-XL pref/alt/hidden labels using
        // the same property name as the plain SKOS variants.
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RULE_INF);
        try {
            model.add(SKOSXL.prefLabel, RDFS.subPropertyOf, SKOS.prefLabel);
            model.add(SKOSXL.altLabel, RDFS.subPropertyOf, SKOS.altLabel);
            model.add(SKOSXL.hiddenLabel, RDFS.subPropertyOf, SKOS.hiddenLabel);
            // Since we use the same graph label for narrower/broader, enabling inverse
            // inference on the model means we get consistent behaviour when syncing
            // these relationships, regardless of how they are defined in the RDF...
            model.add(SKOS.broader, OWL.inverseOf, SKOS.narrower);

            model.read(ios, null, format);
            OntClass conceptClass = model.getOntClass(SkosRDFVocabulary.CONCEPT.getURI().toString());
            logger.debug("in import file: {}", SkosRDFVocabulary.CONCEPT.getURI());
            Map<Resource, Concept> imported = Maps.newHashMap();

            ExtendedIterator<? extends OntResource> itemIterator = conceptClass.listInstances();
            try {
                while (itemIterator.hasNext()) {
                    Resource item = itemIterator.next();

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
                            log.addError(item.toString(), validationError.getMessage());
                        } else {
                            throw validationError;
                        }
                    }
                }
            } finally {
                itemIterator.close();
            }

            for (Map.Entry<Resource, Concept> pair : imported.entrySet()) {
                hookupRelationships(pair.getKey(), pair.getValue(), imported);
            }

            for (Concept concept : imported.values()) {
                vocabulary.addItem(concept);
                concept.setPermissionScope(vocabulary);
            }

            if (log.hasDoneWork()) {
                eventContext.commit();
            }

            return log;
        } finally {
            model.close();
        }
    }

    private Mutation<Concept> importConcept(Resource item) throws ValidationError {
        logger.debug("Importing: {}", item);
        Bundle.Builder builder = Bundle.Builder.withClass(EntityClass.CVOC_CONCEPT)
                .addDataValue(Ontology.IDENTIFIER_KEY, getId(URI.create(item.getURI())))
                .addDataValue(Ontology.URI_KEY, item.getURI());

        for (Map.Entry<String, URI> prop : SkosRDFVocabulary.GENERAL_PROPS.entrySet()) {
            for (RDFNode target : getObjectWithPredicate(item, prop.getValue())) {
                if (target.isLiteral()) {
                    if (prop.getKey().equals("latitude/longitude")) {
                        String[] latLon = target.asLiteral().getString().split(",");
                        if (latLon.length > 1) {
                            builder.addDataValue("latitude", latLon[0]);
                            builder.addDataValue("longitude", latLon[1]);
                        }
                    } else {
                        builder.addDataMultiValue(prop.getKey(), target.asLiteral().getString());
                    }
                } else {
                    builder.addDataMultiValue(prop.getKey(), target.toString());
                }
            }
        }

        Map<AuthoritativeItem, String> linkedConcepts = Maps.newHashMap();

        List<Bundle> unknown = getAdditionalRelations(item, linkedConcepts);
        for (Bundle description : getDescriptions(item)) {
            Bundle withRels = description
                    .withRelations(Ontology.HAS_UNKNOWN_PROPERTY, unknown);
            builder.addRelation(Ontology.DESCRIPTION_FOR_ENTITY, withRels);
        }

        Mutation<Concept> mut = dao.createOrUpdate(builder.build(), Concept.class);
        createLinks(mut.getNode(), linkedConcepts);
        return mut;
    }

    private void createLinks(Concept unit, Map<AuthoritativeItem, String> linkedConcepts) {
        for (AuthoritativeItem concept : linkedConcepts.keySet()) {
            try {
                String relType = linkedConcepts.get(concept);
                String typeKey = relType.substring(0, relType.indexOf(":"));
                String typeValue = relType.substring(relType.indexOf(":") + 1);
                Bundle data = linkTemplate.withDataValue(typeKey, typeValue);
                Optional<Link> existing = findLink(unit, concept, data);
                if (!existing.isPresent()) {
                    Link link = api.create(data, Link.class);
                    unit.addLink(link);
                    concept.addLink(link);
                }
            } catch (ValidationError | PermissionDenied | DeserializationError | SerializationError ex) {
                logger.error("Unexpected error creating relationship link", ex);
            }
        }
    }

    private Optional<Link> findLink(Described unit, Linkable target, Bundle data) throws SerializationError {
        for (Link link : unit.getLinks()) {
            for (Linkable connected : link.getLinkTargets()) {
                if (target.equals(connected)
                        && mergeSerializer.entityToBundle(link).equals(data)) {
                    return Optional.of(link);
                }
            }
        }
        return Optional.empty();
    }

    private List<Bundle> getAdditionalRelations(Resource item, Map<AuthoritativeItem, String> linkedItems) {
        List<Bundle> unknown = Lists.newArrayList();

        for (Map.Entry<String, URI> rel : SkosRDFVocabulary.RELATION_PROPS.entrySet()) {
            for (RDFNode annotation : getObjectWithPredicate(item, rel.getValue())) {
                if (annotation.isLiteral()) {
                    unknown.add(Bundle.Builder.withClass(EntityClass.UNKNOWN_PROPERTY)
                            .addDataValue(rel.getKey(), annotation.toString())
                            .build());
                } else {
                    if (rel.getKey().startsWith("skos:") || rel.getKey().startsWith("sem:")) {
                        Optional<AuthoritativeItem> found = findRelatedConcept(annotation.toString());
                        if (found.isPresent()) {
                            linkedItems.put(found.get(), rel.getKey());
                        } else {
                            unknown.add(Bundle.Builder.withClass(EntityClass.UNKNOWN_PROPERTY)
                                    .addDataValue(rel.getKey(), annotation.toString())
                                    .build());
                        }
                    }
                }
            }
        }
        return unknown;
    }

    private Optional<AuthoritativeItem> findRelatedConcept(String name) {
        if (name != null) {
            String[] domains = name.split("/");
            if (domains.length > 2) {
                String cvocId = domains[domains.length - 2];
                String conceptId = domains[domains.length - 1];
                AuthoritativeSet referredSet;
                try {
                    GraphManager manager = GraphManagerFactory.getInstance(framedGraph);
                    referredSet = manager.getEntity(cvocId, AuthoritativeSet.class);
                    for (AuthoritativeItem authItem : referredSet.getAuthoritativeItems()) {
                        if (authItem.getIdentifier().equals(conceptId)) {
                            return Optional.of(authItem);
                        }
                    }
                } catch (ItemNotFound ex) {
                    logger.error("AuthoritativeSet with id {} not found: {}", cvocId, ex.getMessage());
                }
            }
        }
        return Optional.empty();
    }

    private List<RDFNode> getObjectWithPredicate(Resource item, final URI propUri) {
        // NB: this should be possible with simply item.listProperties(propUri)
        // but for some reason that doesn't work... I can't grok why.
        return item.listProperties().filterKeep(statement ->
                statement.getPredicate()
                        .hasURI(propUri.toString()))
                .mapWith(Statement::getObject).toList();
    }

    private void connectRelation(Concept current, Resource item, Map<Resource, Concept> others,
            URI propUri, Function<Concept, Iterable<Concept>> getter,
            BiConsumer<Concept, Concept> addFunc, BiConsumer<Concept, Concept> dropFunc) {
        Set<Concept> existingRelations = Sets.newHashSet(getter.apply(current));
        Set<Concept> newRelations = getObjectWithPredicate(item, propUri)
                .stream()
                .filter(RDFNode::isResource)
                .map(n -> others.get(n.asResource()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!existingRelations.equals(newRelations)) {
            logger.debug("Updating relations for concept: {}: {} -> {} => {}",
                    propUri, current.getId(), existingRelations, newRelations);

            Sets.difference(existingRelations, newRelations)
                    .forEach(e -> dropFunc.accept(current, e));
            Sets.difference(newRelations, existingRelations)
                    .forEach(n -> addFunc.accept(current, n));
        }
    }

    private void hookupRelationships(Resource item, Concept current, Map<Resource, Concept> conceptMap) {
        connectRelation(current, item, conceptMap, SkosRDFVocabulary.BROADER.getURI(),
                Concept::getBroaderConcepts, Concept::addBroaderConcept, Concept::removeBroaderConcept);
        connectRelation(current, item, conceptMap, SkosRDFVocabulary.NARROWER.getURI(),
                Concept::getNarrowerConcepts, Concept::addNarrowerConcept, Concept::removeNarrowerConcept);
        connectRelation(current, item, conceptMap, SkosRDFVocabulary.RELATED.getURI(),
                Concept::getRelatedConcepts, Concept::addRelatedConcept, Concept::removeRelatedConcept);
    }

    private List<StringValue> getReifiedObjectValue(Resource item, URI propUri) {
        List<StringValue> values = Lists.newArrayList();
        for (RDFNode node : getObjectWithPredicate(item, propUri)) {
            if (node.isLiteral()) {
                values.add(new StringValue(node.asLiteral().getString(), node.asLiteral().getLanguage()));
            } else {
                Stream.of(SKOSXL.literalForm, RDF.value)
                        .map(prop -> node.asResource().getProperty(prop))
                        .filter(Objects::nonNull)
                        .map(Statement::getObject)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(object -> values.add(new StringValue(
                                object.asLiteral().getString(), object.asLiteral().getLanguage())));
            }
        }
        return values;
    }

    private List<Bundle> getDescriptions(Resource item) {
        List<Bundle> descriptions = Lists.newArrayList();

        for (StringValue property : getReifiedObjectValue(item, SkosRDFVocabulary.PREF_LABEL.getURI())) {

            Bundle.Builder builder = Bundle.Builder.withClass(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            String langCode2Letter = property.getLang();
            String langCode3Letter = getLanguageCode(langCode2Letter, defaultLang);
            Optional<String> descCode = getScriptCode(langCode2Letter);

            builder.addDataValue(Ontology.NAME_KEY, property.getValue())
                    .addDataValue(Ontology.LANGUAGE, langCode3Letter);
            descCode.ifPresent(code -> builder.addDataValue(Ontology.IDENTIFIER_KEY, code));

            for (Map.Entry<String, List<URI>> prop : SkosRDFVocabulary.LANGUAGE_PROPS.entrySet()) {
                List<String> values = Lists.newArrayList();
                for (URI uri : prop.getValue()) {
                    for (StringValue target : getReifiedObjectValue(item, uri)) {
                        String propLang2Letter = target.getLang();
                        String propLanguageCode = getLanguageCode(propLang2Letter, defaultLang);
                        Optional<String> propDescCode = getScriptCode(propLang2Letter);
                        if (propLanguageCode.equals(langCode3Letter) && propDescCode.equals(descCode)) {
                            values.add(target.getValue());
                        }
                    }
                }
                if (!values.isEmpty()) {
                    // Sorting the related literal values in
                    // natural order gives consistency between
                    // import/export round-trips. The data is
                    // otherwise unsorted.
                    Collections.sort(values);
                    builder.addDataValue(prop.getKey(), values);
                }
            }
            Bundle bundle = builder.build();
            logger.trace(bundle.toJson());
            descriptions.add(bundle);
        }

        // Hack: if there's only one description it means we've only
        // got one prefLabel with a language tag. In this case get
        // a list of altLabels in all languages :(
        if (descriptions.size() == 1) {
            List<String> all = getReifiedObjectValue(item, SkosRDFVocabulary.ALT_LABEL.getURI())
                    .stream()
                    .map(StringValue::getValue)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            if (!all.isEmpty()) {
                descriptions.set(0, descriptions.get(0).withDataValue(SkosMultilingual.altLabel.toString(), all));
            }
        }

        return descriptions;
    }

    private static String getLanguageCode(String langCode2Letter, String defaultLang) {
        if (langCode2Letter == null || langCode2Letter.trim().isEmpty()) {
            return defaultLang;
        }
        List<String> parts = codeSplitter.splitToList(langCode2Letter);
        if (parts.isEmpty()) {
            return defaultLang;
        } else {
            return LanguageHelpers.iso639DashTwoCode(parts.get(0));
        }
    }

    private static Optional<String> getScriptCode(String langCode) {
        List<String> parts = codeSplitter.splitToList(langCode);
        return parts.size() > 1 ? Optional.of(parts.get(1)) : Optional.empty();
    }

    private String getId(URI uri) {
        if (baseURI != null && suffix != null && uri.toString().startsWith(baseURI)) {
            String sub = uri.toString().substring(baseURI.length());
            return sub.substring(0, sub.lastIndexOf(suffix));
        } else if (baseURI != null && uri.toString().startsWith(baseURI)) {
            return uri.toString().substring(baseURI.length());
        } else if (uri.getFragment() != null) {
            return uri.getFragment();
        } else {
            return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                    + (uri.getQuery() != null ? uri.getQuery() : "")
                    + (uri.getFragment() != null ? uri.getFragment() : "");
        }
    }

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.empty() : Optional.of(msg);
    }
}
