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

package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import java.util.Properties;

/**
 * Add a user.
 */
public class EntityAdd extends BaseCommand implements Command {

    final static String NAME = "add";

    /**
     * Constructor.
     */
    public EntityAdd() {
    }

    @Override
    @SuppressWarnings("static-access")
    protected void setCustomOptions() {
        options.addOption(OptionBuilder.withArgName("property=value")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription("Add a property with the given value")
                .create("P"));
        options.addOption(new Option("scope", true,
                "Identifier of scope to create item in, i.e. a repository"));
        options.addOption(new Option("u", "update", false,
                "Update item if it already exists"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("log", true,
                "Log message for create action."));
    }

    @Override
    public String getHelp() {
        return String.format("Usage: %s <type> [OPTIONS] [-Pkey=value]", NAME);
    }

    @Override
    public String getUsage() {
        return "Create a new entity with the given id and properties";
    }

    @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        // Find the agent
        PermissionScope scope = SystemScope.getInstance();
        if (cmdLine.hasOption("scope")) {
            scope = manager.getFrame(cmdLine.getOptionValue("scope"), PermissionScope.class);
        }

        // Find the user
        UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                UserProfile.class);

        String typeName = (String) cmdLine.getArgList().get(0);
        EntityClass entityClass = EntityClass.withName(typeName);
        Properties properties = cmdLine.getOptionProperties("P");

        Bundle.Builder builder = Bundle.Builder.withClass(entityClass);
        for (Object prop : properties.keySet()) {
            builder.addDataValue((String) prop, properties.getProperty((String) prop));
        }
        Bundle bundle = builder.build();
        String id = entityClass.getIdgen().generateId(scope.idPath(), bundle);

        try {
            createItem(graph, cmdLine, id, bundle, scope, user, logMessage);
            graph.getBaseGraph().commit();
        } catch (ValidationError e) {
            graph.getBaseGraph().rollback();
            System.err.printf("A user a id: '%s' already exists%n", id);
            return CmdEntryPoint.RetCode.BAD_DATA.getCode();
        } catch (PermissionDenied e) {
            graph.getBaseGraph().rollback();
            System.err.printf("User %s does not have permission to perform that action.%n", user.getId());
            return CmdEntryPoint.RetCode.BAD_PERMS.getCode();
        } catch (DeserializationError e) {
            graph.getBaseGraph().rollback();
            System.err.println(e.getMessage());
            return CmdEntryPoint.RetCode.BAD_DATA.getCode();
        }

        return 0;
    }

    // Suppressing warnings here because we throw a RuntimeException if the
    // item class is not of an acceptable type.
    @SuppressWarnings("unchecked")
    public void createItem(final FramedGraph<? extends TransactionalGraph> graph,
            CommandLine cmdLine, String id, Bundle bundle,
            PermissionScope scope, UserProfile user, String logMessage) throws DeserializationError,
            ValidationError, PermissionDenied {

        if (!AccessibleEntity.class.isAssignableFrom(bundle.getBundleClass())) {
            throw new DeserializationError("Item class: " + bundle.getBundleClass().getSimpleName() +
                    " is not a first-class database item");
        }

        LoggingCrudViews<?> view = new LoggingCrudViews(graph, bundle.getBundleClass(), scope);
        if (cmdLine.hasOption("update")) {
            view.createOrUpdate(bundle.withId(id), user, getLogMessage(logMessage));
        } else {
            view.create(bundle.withId(id), user, getLogMessage(logMessage));
        }
    }
}
