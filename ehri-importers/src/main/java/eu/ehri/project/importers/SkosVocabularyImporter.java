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
import org.semanticweb.skos.*;
import org.semanticweb.skosapibinding.SKOSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.skos.SKOSRDFVocabulary;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

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

    public static final String DEFAULT_LANG = "eng";

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

            SKOSManager manager = new SKOSManager();
            SKOSDataset vocab = manager.loadDatasetFromPhysicalURI(dataFile.toURI());

            Map<URI, Concept> imported = Maps.newHashMap();

            System.out.println("Concepts found: " + vocab.getSKOSConcepts().size());

            for (SKOSConcept concept : vocab.getSKOSConcepts()) {
                Mutation<Concept> graphConcept = importConcept(concept, vocab);
                imported.put(concept.getURI(), graphConcept.getNode());

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

            for (SKOSConcept concept : vocab.getSKOSConcepts()) {
                hookupRelationships(concept, vocab, imported);
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

    private Mutation<Concept> importConcept(SKOSConcept skosConcept, SKOSDataset dataset) throws ValidationError {
        logger.debug("Importing: {}", skosConcept.getURI());
        Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT)
                .addDataValue(Ontology.IDENTIFIER_KEY, skosConcept.getURI().toString());

        for (Bundle description : getDescriptions(skosConcept, dataset)) {
            builder.addRelation(Ontology.DESCRIPTION_FOR_ENTITY, description);
        }

        return dao.createOrUpdate(builder.build(), Concept.class);
    }

    private void hookupRelationships(SKOSConcept skosConcept, SKOSDataset dataset, Map<URI, Concept> conceptMap) {
        Concept current = conceptMap.get(skosConcept.getURI());
        for (SKOSAnnotation broaderAnn : skosConcept.getSKOSAnnotationsByURI(dataset,
                SKOSRDFVocabulary.BROADER.getURI())) {
            Concept broader = conceptMap.get(broaderAnn.getAnnotationValue().getURI());
            if (broader != null) {
                broader.addNarrowerConcept(current);
            }
        }

        for (SKOSAnnotation narrowerAnn : skosConcept.getSKOSAnnotationsByURI(dataset,
                SKOSRDFVocabulary.NARROWER.getURI())) {
            SKOSEntity annotationValue = narrowerAnn.getAnnotationValue();
            if (annotationValue != null) {
                Concept narrower = conceptMap.get(annotationValue.getURI());
                if (narrower != null) {
                    current.addNarrowerConcept(narrower);
                }
            }
        }

        for (SKOSAnnotation relatedAnn : skosConcept.getSKOSAnnotationsByURI(dataset,
                SKOSRDFVocabulary.RELATED.getURI())) {
            Concept related = conceptMap.get(relatedAnn.getAnnotationValue().getURI());
            if (related != null) {
                current.addRelatedConcept(related);
            }
        }

    }

    private List<Bundle> getDescriptions(SKOSConcept skosConcept, SKOSDataset dataset) {

        Map<SKOSRDFVocabulary, String> props = ImmutableMap.<SKOSRDFVocabulary,String>builder()
               .put(SKOSRDFVocabulary.PREFLABEL, Ontology.CONCEPT_ALTLABEL)
               .put(SKOSRDFVocabulary.ALTLABEL, Ontology.CONCEPT_ALTLABEL)
               .put(SKOSRDFVocabulary.HIDDENLABEL, Ontology.CONCEPT_HIDDENLABEL)
               .put(SKOSRDFVocabulary.DEFINITION, Ontology.CONCEPT_DEFINITION)
               .put(SKOSRDFVocabulary.SCOPENOTE, Ontology.CONCEPT_SCOPENOTE)
               .put(SKOSRDFVocabulary.NOTE, Ontology.CONCEPT_NOTE)
               .put(SKOSRDFVocabulary.EDITORIALNOTE, Ontology.CONCEPT_EDITORIAL_NOTE)
               .build();

        List<Bundle> descriptions = Lists.newArrayList();

        for (SKOSAnnotation prefLabel : skosConcept.getSKOSAnnotationsByURI(dataset,
                SKOSRDFVocabulary.PREFLABEL.getURI())) {
            Bundle.Builder builder = new Bundle.Builder(EntityClass.CVOC_CONCEPT_DESCRIPTION);

            SKOSLiteral literalPrefName = prefLabel.getAnnotationValueAsConstant();
            String lang = literalPrefName.getAsSKOSUntypedLiteral().getLang();
            String languageCode = (lang == null || lang.trim().isEmpty())
                    ? DEFAULT_LANG
                    : Helpers.iso639DashTwoCode(lang);

            builder.addDataValue(Ontology.NAME_KEY, literalPrefName.getLiteral())
                    .addDataValue(Ontology.LANGUAGE, languageCode);

            for (Map.Entry<SKOSRDFVocabulary, String> prop : props.entrySet()) {
                List<String> values = Lists.newArrayList();
                for (SKOSAnnotation propVal : skosConcept.getSKOSAnnotationsByURI(dataset,
                        prop.getKey().getURI())) {
                    SKOSLiteral literalProp = propVal.getAnnotationValueAsConstant();
                    String propLang = literalProp.getAsSKOSUntypedLiteral().getLang();
                    String propLanguageCode = (propLang == null || propLang.trim().isEmpty())
                            ? DEFAULT_LANG
                            : Helpers.iso639DashTwoCode(propLang);
                    if (propLanguageCode.equals(languageCode)) {
                        values.add(literalProp.getLiteral());
                    }
                }
                if (!values.isEmpty()) {
                    builder.addDataValue(prop.getValue(), values);
                }
            }
            descriptions.add(builder.build());
        }

        return descriptions;
    }

    private Optional<String> getLogMessage(String msg) {
        return msg.trim().isEmpty() ? Optional.<String>absent() : Optional.of(msg);
    }
}
