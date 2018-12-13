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

import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Class that provides a front-end for importing XML files like EAD and EAC and
 * nested lists of EAD documents into the graph.
 */
public class SaxImportManager extends AbstractImportManager {

    private static final Logger logger = LoggerFactory.getLogger(SaxImportManager.class);

    private static final Config config = ConfigFactory.load();
    private final Class<? extends SaxXmlHandler> handlerClass;
    private final XmlImportProperties properties;
    private final List<ImportCallback> extraCallbacks;

    /**
     * Constructor.
     *
     * @param graph    the framed graph
     * @param scope    the permission scope
     * @param actioner the actioner
     */
    public SaxImportManager(FramedGraph<?> graph,
            PermissionScope scope,
            Actioner actioner,
            boolean tolerant,
            boolean allowUpdates,
            String defaultLang,
            Class<? extends ItemImporter<?,?>> importerClass,
            Class<? extends SaxXmlHandler> handlerClass,
            XmlImportProperties properties,
            List<ImportCallback> callbacks) {
        super(graph, scope, actioner, tolerant, allowUpdates, defaultLang, importerClass);
        this.handlerClass = handlerClass;
        this.properties = properties;
        this.extraCallbacks = Lists.newArrayList(callbacks);
        logger.debug("importer used: {}", importerClass);
        logger.debug("handler used: {}", handlerClass);
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
            boolean tolerant,
            boolean allowUpdates,
            String defaultLang,
            Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass,
            List<ImportCallback> callbacks) {
        this(graph, scope, actioner, tolerant, allowUpdates, defaultLang, importerClass, handlerClass, null,
                callbacks);
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
            boolean tolerant,
            boolean allowUpdates,
            String defaultLang,
            Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass,
            XmlImportProperties properties) {
        this(graph, scope, actioner, tolerant, allowUpdates, defaultLang, importerClass, handlerClass,
                properties,
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
            Class<? extends ItemImporter<?,?>> importerClass, Class<? extends SaxXmlHandler> handlerClass) {
        this(graph, scope, actioner, false, false,
                config.getString("defaultLang"),
                importerClass, handlerClass, Lists.<ImportCallback>newArrayList());
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param stream  an input stream
     * @param context the event context
     * @param log     a logger object
     */
    @Override
    protected void importInputStream(final InputStream stream, final String tag, final ActionManager.EventContext context,
            final ImportLog log) throws IOException, ValidationError, InputParseError {
        try {
            ItemImporter<?, ?> importer = importerClass
                    .getConstructor(FramedGraph.class, PermissionScope.class,
                            Actioner.class, ImportLog.class)
                    .newInstance(framedGraph, permissionScope, actioner, log);

            for (ImportCallback callback : extraCallbacks) {
                importer.addCallback(callback);
            }

            importer.addCallback(mutation -> defaultImportCallback(log, context, mutation));
            importer.addErrorCallback(ex -> defaultErrorCallback(log, ex));

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            if (isTolerant()) {
                logger.trace("Turning off validation and setting schema to null");
                spf.setValidating(false);
                spf.setSchema(null);
            }
            logger.trace("isValidating: {}", spf.isValidating());


            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            //TODO decide which handler to use, HandlerFactory? now part of constructor ...
            SaxXmlHandler handler = properties != null
                    ? handlerClass.getConstructor(XMLReader.class, ItemImporter.class, String.class, XmlImportProperties.class)
                    .newInstance(xmlReader, importer, defaultLang, properties)
                    : handlerClass.getConstructor(XMLReader.class, ItemImporter.class, String.class)
                    .newInstance(xmlReader, importer, defaultLang);

            saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            InputSource src = new InputSource(stream);
            src.setSystemId(tag);
            saxParser.parse(src, handler);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                NoSuchMethodException | SecurityException |
                ParserConfigurationException e) {
            // In normal operation these should not be thrown
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // Something was wrong with the XML...
            throw new InputParseError(e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ValidationError) {
                throw (ValidationError)e.getCause();
            } else {
                throw e;
            }
        }
    }

    public SaxImportManager withProperties(String properties) {
        XmlImportProperties xmlImportProperties = properties == null ? null : new XmlImportProperties(properties);
        return new SaxImportManager(framedGraph, permissionScope, actioner, tolerant, allowUpdates, defaultLang,
                importerClass, handlerClass, xmlImportProperties, extraCallbacks);
    }

    public SaxImportManager setTolerant(boolean tolerant) {
        return new SaxImportManager(framedGraph, permissionScope, actioner, tolerant,
                allowUpdates, defaultLang, importerClass, handlerClass, properties, extraCallbacks);
    }

    public SaxImportManager allowUpdates(boolean allowUpdates) {
        return new SaxImportManager(framedGraph, permissionScope, actioner, tolerant,
                allowUpdates, defaultLang, importerClass, handlerClass, properties, extraCallbacks);
    }

    public SaxImportManager withScope(PermissionScope scope) {
        return new SaxImportManager(framedGraph, scope, actioner, tolerant, allowUpdates, defaultLang,
                importerClass, handlerClass, properties, extraCallbacks);
    }

    public SaxImportManager setDefaultLang(String defaultLang) {
        return new SaxImportManager(framedGraph, permissionScope, actioner, tolerant,
                allowUpdates, defaultLang, importerClass, handlerClass, properties, extraCallbacks);
    }

    public SaxImportManager withCallback(ImportCallback callback) {
        List<ImportCallback> newCbs = Lists.newArrayList(extraCallbacks);
        newCbs.add(callback);
        return new SaxImportManager(framedGraph, permissionScope, actioner, tolerant,
                allowUpdates, defaultLang, importerClass, handlerClass, properties, newCbs);
    }
}
