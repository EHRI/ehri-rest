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

package eu.ehri.project.importers.managers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Class that provides a front-end for importing XML files like EAD and EAC and
 * nested lists of EAD documents into the graph.
 */
public class SaxImportManager extends AbstractImportManager {

    private static final Logger logger = LoggerFactory.getLogger(SaxImportManager.class);

    private final Class<? extends SaxXmlHandler> handlerClass;
    private final Optional<XmlImportProperties> properties;
    private final List<ImportCallback> extraCallbacks;

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    the permission scope
     * @param actioner the actioner
     */
    public SaxImportManager(FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass,
            Optional<XmlImportProperties> properties,
            List<ImportCallback> callbacks) {
        super(graph, scope, actioner, importerClass);
        this.handlerClass = handlerClass;
        this.properties = properties;
        this.extraCallbacks = Lists.newArrayList(callbacks);
        logger.info("importer used: " + importerClass);
        logger.info("handler used: " + handlerClass);
    }

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    a permission scope
     * @param actioner the actioner
     */
    public SaxImportManager(FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass,
            List<ImportCallback> callbacks) {
        this(graph, scope, actioner, importerClass, handlerClass, Optional.<XmlImportProperties>absent(), callbacks);
    }

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    a permission scope
     * @param actioner the actioner
     */
    public SaxImportManager(FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass,
            XmlImportProperties properties) {
        this(graph, scope, actioner, importerClass, handlerClass, Optional.fromNullable(properties),
                Lists.<ImportCallback>newArrayList());
    }

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    a permission scope
     * @param actioner the actioner
     */
    public SaxImportManager(FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner,
            Class<? extends AbstractImporter> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        this(graph, scope, actioner, importerClass, handlerClass, Lists.<ImportCallback>newArrayList());
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param ios          an input stream
     * @param eventContext the event context
     * @param log          a logger object
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidXmlDocument
     */
    @Override
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError, InputParseError {

        try {
            AbstractImporter<Map<String, Object>> importer = importerClass
                    .getConstructor(FramedGraph.class, PermissionScope.class,
                            ImportLog.class).newInstance(framedGraph, permissionScope, log);

            for (ImportCallback callback : extraCallbacks) {
                importer.addCallback(callback);
            }

            // Add housekeeping callbacks for the log object...
            importer.addCallback(new ImportCallback() {
                public void itemImported(Mutation<? extends Accessible> mutation) {
                    switch (mutation.getState()) {
                        case CREATED:
                            logger.info("Item created: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addCreated();
                            break;
                        case UPDATED:
                            logger.info("Item updated: {}", mutation.getNode().getId());
                            eventContext.addSubjects(mutation.getNode());
                            log.addUpdated();
                            break;
                        default:
                            log.addUnchanged();
                    }
                }
            });
            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
            SaxXmlHandler handler = properties.isPresent()
                    ? handlerClass.getConstructor(AbstractImporter.class, XmlImportProperties.class)
                    .newInstance(importer, properties.get())
                    : handlerClass.getConstructor(AbstractImporter.class).newInstance(importer);

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            if (isTolerant()) {
                logger.debug("Turning off validation and setting schema to null");
                spf.setValidating(false);
                spf.setSchema(null);
            }
            logger.debug("isValidating: " + spf.isValidating());
            SAXParser saxParser = spf.newSAXParser();
            saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            saxParser.parse(ios, handler);
        } catch (InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException |
                NoSuchMethodException | SecurityException |
                ParserConfigurationException e) {
            logger.error("{}: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (SAXException e) {
            logger.error("SAXException: " + e.getMessage());
            throw new InputParseError(e);
        }
    }

    public SaxImportManager withProperties(XmlImportProperties properties) {
        return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass,
                Optional.of(properties), extraCallbacks);
    }

    public SaxImportManager withProperties(String properties) {
        if (properties == null) {
            return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass);
        } else {
            XmlImportProperties xmlImportProperties = new XmlImportProperties(properties);
            return new SaxImportManager(framedGraph, permissionScope, actioner, importerClass, handlerClass,
                    Optional.of(xmlImportProperties), extraCallbacks);
        }
    }
}
