/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.ActionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author linda
 */
public class CsvImportManager extends XmlImportManager {

    public static final Character VALUE_DELIMITER = ';';

    private static final Logger logger = LoggerFactory.getLogger(CsvImportManager.class);
    private AbstractImporter<Map<String, Object>> importer;
    private Class<? extends AbstractImporter> importerClass;

    public CsvImportManager(FramedGraph<Neo4jGraph> framedGraph,
            final PermissionScope permissionScope, final Actioner actioner, Class<? extends AbstractImporter> importerClass) {
        super(framedGraph, permissionScope, actioner);
        this.importerClass = importerClass;
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param ios
     * @param eventContext
     * @param log
     * @throws IOException
     * @throws ValidationError
     * @throws InvalidInputFormatError
     */
    @Override
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError, InvalidInputFormatError {

        try {
            importer = importerClass.getConstructor(FramedGraph.class, PermissionScope.class,
                    ImportLog.class).newInstance(framedGraph, permissionScope, log);
            logger.info("importer of class " + importer.getClass());
            importer.addCreationCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("ImportCallback: itemImported creation " + item.getId());
                    eventContext.addSubjects(item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("ImportCallback: itemImported updated");
                    eventContext.addSubjects(item);
                    log.addUpdated();
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
                importer.importItem(dataMap);
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
