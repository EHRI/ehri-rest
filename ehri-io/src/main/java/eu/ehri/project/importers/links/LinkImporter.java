package eu.ehri.project.importers.links;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.utils.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LinkImporter {

    private static final Logger logger = LoggerFactory.getLogger(LinkImporter.class);

    private static final List<String> COLUMNS = Lists.newArrayList(
            "source ID",
            "target ID",
            "access point ID",
            "type",
            "field",
            "description"
    );

    private final FramedGraph<?> framedGraph;
    private final GraphManager manager;
    private final Actioner actioner;
    private final ActionManager actionManager;
    private final BundleManager bundleManager;
    private final boolean tolerant;

    public LinkImporter(FramedGraph<?> framedGraph, Actioner actioner, boolean tolerant) {
        this.framedGraph = framedGraph;
        this.manager = GraphManagerFactory.getInstance(framedGraph);
        this.actioner = actioner;
        this.actionManager = new ActionManager(framedGraph);
        this.bundleManager = new BundleManager(framedGraph);
        this.tolerant = tolerant;
    }

    public ImportLog importLinks(Table table, String logMessage) throws DeserializationError {

        if (!table.rows().isEmpty() && table.rows().get(0).size() != COLUMNS.size()) {
            throw new DeserializationError(
                    String.format("Input CSV must have 6 columns: %s. " +
                                    "Leave columns blank where not applicable.",
                    Joiner.on(", ").join(COLUMNS)));
        }

        ImportLog log = new ImportLog(logMessage);
        ActionManager.EventContext eventContext = actionManager.newEventContext(
                actioner,
                EventTypes.ingest,
                Optional.ofNullable(logMessage));

        for (int i = 0; i < table.rows().size(); i++) {
            List<String> row = table.rows().get(i);
            String from = row.get(0);
            String to = row.get(1);
            String ap = row.get(2);
            String type = row.get(3);
            String field = row.get(4);
            String desc = row.get(5);
            Bundle.Builder builder = Bundle.Builder.withClass(EntityClass.LINK)
                    .addDataValue(Ontology.LINK_HAS_TYPE, type);
            if (!field.trim().isEmpty()) {
                builder.addDataValue(Ontology.LINK_HAS_FIELD, field);
            }
            if (!desc.trim().isEmpty()) {
                builder.addDataValue(Ontology.LINK_HAS_DESCRIPTION, desc);
            }
            Bundle bundle = builder.build();

            try {
                Linkable t1 = manager.getEntity(from, Linkable.class);
                Linkable t2 = manager.getEntity(to, Linkable.class);

                Link link = bundleManager.create(bundle, Link.class);
                link.addLinkTarget(t1);
                link.addLinkTarget(t2);
                link.setLinker(actioner.as(Accessor.class));

                if (!ap.trim().isEmpty()) {
                    AccessPoint accessPoint = manager.getEntity(ap, AccessPoint.class);
                    link.addLinkBody(accessPoint);
                }

                eventContext.addSubjects(link);
                log.addCreated("-", link.getId());
            } catch (ItemNotFound e) {
                logger.error("Item not found at row {}: {}", i, e.getValue());
                log.addError(e.getValue(), e.getMessage());
                if (!tolerant) {
                    throw new DeserializationError(
                            String.format("Item ID '%s' not found at row: %d", e.getValue(), i));
                }
            } catch (ValidationError e) {
                log.addError(from, e.getMessage());
                logger.error("Deserialization error at row {}: {}", i, e.getMessage());
                if (!tolerant) {
                    throw new DeserializationError(
                            String.format("Error validating link at row %d: %s", i,
                                    e.getMessage()));
                }
            }
        }
        if (log.hasDoneWork()) {
            eventContext.commit();
        }

        return log;
    }
}
