/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.PostImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

    private final Class<? extends SaxXmlHandler> handlerClass;
    private final ImportOptions options;

    private SaxImportManager(FramedGraph<?> graph,
                             PermissionScope scope,
                             Actioner actioner,
                             Class<? extends ItemImporter<?, ?>> importerClass,
                             Class<? extends SaxXmlHandler> handlerClass,
                             ImportOptions options,
                             List<PreImportCallback> preCallbacks,
                             List<PostImportCallback> callbacks) {
        super(graph, scope, actioner, importerClass, options, preCallbacks, callbacks);
        this.handlerClass = handlerClass;
        this.options = options;
        logger.debug("importer used: {}", importerClass);
        logger.debug("handler used: {}", handlerClass);
    }

    private SaxImportManager(FramedGraph<?> graph,
                             PermissionScope scope,
                             Actioner actioner,
                             Class<? extends ItemImporter<?, ?>> importerClass,
                             Class<? extends SaxXmlHandler> handlerClass, ImportOptions options) {
        this(graph, scope, actioner, importerClass, handlerClass, options, Lists.newArrayList(), Lists.newArrayList());
    }

    /**
     * Factory method.
     *
     * @param graph         the framed graph
     * @param scope         the permission scope
     * @param actioner      the actioner
     * @param importerClass the import class
     * @param handlerClass  the handler class
     * @param options       an ImportOptions instance
     * @param callbacks     a list of ImportCallback instances
     * @return a new import manager
     */
    public static SaxImportManager create(FramedGraph<?> graph,
                                          PermissionScope scope,
                                          Actioner actioner,
                                          Class<? extends ItemImporter<?, ?>> importerClass,
                                          Class<? extends SaxXmlHandler> handlerClass,
                                          ImportOptions options,
                                          List<PreImportCallback> preCallbacks,
                                          List<PostImportCallback> callbacks) {
        return new SaxImportManager(graph, scope, actioner, importerClass, handlerClass, options, preCallbacks, callbacks);
    }

    /**
     * Factory method.
     *
     * @param graph         the framed graph
     * @param scope         a permission scope
     * @param actioner      the actioner
     * @param importerClass the import class
     * @param handlerClass  the handler class
     * @param options       an import options instance
     * @return a new import manager
     */
    public static SaxImportManager create(FramedGraph<?> graph,
                                          PermissionScope scope,
                                          Actioner actioner,
                                          Class<? extends ItemImporter<?, ?>> importerClass,
                                          Class<? extends SaxXmlHandler> handlerClass,
                                          ImportOptions options) {
        return new SaxImportManager(graph, scope, actioner, importerClass, handlerClass, options);
    }

    @Override
    protected void importInputStream(final InputStream stream, final String tag, final ActionManager.EventContext context,
                                     final ImportLog log) throws IOException, ValidationError, InputParseError {
        try {
            ItemImporter<?, ?> importer = importerClass
                    .getConstructor(FramedGraph.class, PermissionScopeFinder.class, Actioner.class, ImportOptions.class, ImportLog.class)
                    .newInstance(framedGraph, scopeFinder, actioner, options, log);

            registerCallbacks(importer);
            importer.addPostCallback(mutation -> defaultImportCallback(log, tag, context, mutation));
            importer.addErrorCallback(ex -> defaultErrorCallback(log, ex));

            SaxXmlHandler handler = handlerClass.getConstructor(ItemImporter.class, ImportOptions.class)
                    .newInstance(importer, options);

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            if (isTolerant()) {
                logger.trace("Turning off validation and setting schema to null");
                spf.setValidating(false);
                spf.setSchema(null);
            }
            logger.trace("isValidating: {}", spf.isValidating());
            SAXParser saxParser = spf.newSAXParser();
            saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            InputSource src = new InputSource(stream);
            src.setPublicId(tag);
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
                throw (ValidationError) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public SaxImportManager withScope(PermissionScope scope) {
        return create(framedGraph, scope, actioner, importerClass, handlerClass, options, preCallbacks, postCallbacks);
    }

    public SaxImportManager withPreCallback(PreImportCallback callback) {
        List<PreImportCallback> newCbs = Lists.newArrayList(preCallbacks);
        newCbs.add(callback);
        return create(framedGraph, permissionScope, actioner,
                importerClass, handlerClass, options, newCbs, postCallbacks);
    }

    public SaxImportManager withCallback(PostImportCallback callback) {
        List<PostImportCallback> newCbs = Lists.newArrayList(postCallbacks);
        newCbs.add(callback);
        return create(framedGraph, permissionScope, actioner,
                importerClass, handlerClass, options, preCallbacks, newCbs);
    }

    public SaxImportManager withUpdates(boolean updates) {
        return create(framedGraph, permissionScope, actioner, importerClass, handlerClass,
                options.withUpdates(updates),preCallbacks, postCallbacks);
    }
}
