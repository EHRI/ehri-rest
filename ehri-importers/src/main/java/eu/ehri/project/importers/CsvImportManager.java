package eu.ehri.project.importers;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class CsvImportManager extends XmlImportManager {

    public static final Character VALUE_DELIMITER = ';';

    private static final Logger logger = LoggerFactory.getLogger(CsvImportManager.class);
    private Class<? extends XmlImporter> importerClass;

    public CsvImportManager(FramedGraph<? extends TransactionalGraph> framedGraph,
            final PermissionScope permissionScope, final Actioner actioner, Class<? extends XmlImporter> importerClass) {
        super(framedGraph, permissionScope, actioner);
        this.importerClass = importerClass;
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param ios The input stream
     * @param eventContext The event context in which the ingest is happening
     * @param log An import log instance
     * @throws IOException
     * @throws ValidationError
     * @throws InvalidInputFormatError
     */
    @Override
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError, InvalidInputFormatError {

        try {
            XmlImporter importer = importerClass.getConstructor(FramedGraph.class, PermissionScope.class,
                    ImportLog.class).newInstance(framedGraph, permissionScope, log);
            logger.debug("importer of class " + importer.getClass());
            importer.addCreationCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("Item created: {}", item.getId());
                    eventContext.addSubjects(item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("Item updated: {}", item.getId());
                    eventContext.addSubjects(item);
                    log.addUpdated();
                }
            });
            importer.addUnchangedCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    log.addUnchanged();
                }
            });

            CSVReader reader = new CSVReader(new InputStreamReader(ios, "UTF-8"), VALUE_DELIMITER);
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new InvalidInputFormatError("no content found");
            } else {
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = headers[i].replaceAll("\\s", "");
                }
            }

//            importer.checkProperties(headers);
            //per record, call importer.importItem(Map<String, Object> itemData

            String[] data;
            while ((data = reader.readNext()) != null) {
                Map<String, Object> dataMap = Maps.newHashMap();
                for (int i = 0; i < data.length; i++) {
                    SaxXmlHandler.putPropertyInGraph(dataMap, headers[i], data[i]);
                }
                try {
                    importer.importItem(dataMap);
                } catch (ValidationError e) {
                    if (isTolerant()) {
                        logger.error("Validation error importing item: {}", e);
                    } else {
                        throw e;
                    }
                }
            }
            framedGraph.getBaseGraph().commit();
        } catch (ValidationError e) {
            framedGraph.getBaseGraph().rollback();
            throw e;
        } catch (InstantiationException e) {
            framedGraph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            framedGraph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            framedGraph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            framedGraph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }
    }
}
