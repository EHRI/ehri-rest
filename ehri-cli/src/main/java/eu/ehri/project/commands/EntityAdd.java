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
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Properties;

/**
 * Add a user.
 */
public class EntityAdd extends BaseCommand {

    final static String NAME = "add";

    public EntityAdd() {
    }

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder("P")
                .argName("property=value")
                .numberOfArgs(2)
                .valueSeparator()
                .desc("Add a property with the given value")
                .build());
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .required()
                .type(String.class)
                .hasArg().desc("Identifier of user taking action")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for delete action.")
                .build());
        options.addOption(Option.builder()
                .longOpt("scope")
                .hasArg()
                .type(String.class)
                .desc("Identifier of scope to import into, i.e. repository")
                .build());
        options.addOption(Option.builder()
                .longOpt("update")
                .desc("Update item if it already exists")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s <type> [OPTIONS] [-Pkey=value]", NAME);
    }

    @Override
    public String getHelp() {
        return "Create a new entity with the given id and properties";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        // Find the agent
        PermissionScope scope = SystemScope.getInstance();
        if (cmdLine.hasOption("scope")) {
            scope = manager.getEntity(cmdLine.getOptionValue("scope"), PermissionScope.class);
        }

        // Find the user
        UserProfile user = manager.getEntity(cmdLine.getOptionValue("user"),
                UserProfile.class);

        String typeName = cmdLine.getArgList().get(0);
        EntityClass entityClass = EntityClass.withName(typeName);
        Properties properties = cmdLine.getOptionProperties("P");

        Bundle.Builder builder = Bundle.Builder.withClass(entityClass);
        for (Object prop : properties.keySet()) {
            builder.addDataValue((String) prop, properties.getProperty((String) prop));
        }
        Bundle bundle = builder.build();
        String id = entityClass.getIdGen().generateId(scope.idPath(), bundle);

        try {
            createItem(graph, cmdLine, id, bundle, scope, user, logMessage);
        } catch (ValidationError e) {
            System.err.printf("A user a id: '%s' already exists%n", id);
            return CmdEntryPoint.RetCode.BAD_DATA.getCode();
        } catch (PermissionDenied e) {
            System.err.printf("User %s does not have permission to perform that action.%n", user.getId());
            return CmdEntryPoint.RetCode.BAD_PERMS.getCode();
        } catch (DeserializationError e) {
            System.err.println(e.getMessage());
            return CmdEntryPoint.RetCode.BAD_DATA.getCode();
        }

        return 0;
    }

    // Suppressing warnings here because we throw a RuntimeException if the
    // item class is not of an acceptable type.
    @SuppressWarnings("unchecked")
    public void createItem(FramedGraph<?> graph,
            CommandLine cmdLine, String id, Bundle bundle,
            PermissionScope scope, UserProfile user, String logMessage) throws DeserializationError,
            ValidationError, PermissionDenied {

        if (!Accessible.class.isAssignableFrom(bundle.getBundleJavaClass())) {
            throw new DeserializationError("Item class: " + bundle.getBundleJavaClass().getSimpleName() +
                    " is not a first-class database item");
        }

        LoggingCrudViews<?> view = new LoggingCrudViews(graph, bundle.getBundleJavaClass(), scope);
        if (cmdLine.hasOption("update")) {
            view.createOrUpdate(bundle.withId(id), user, getLogMessage(logMessage));
        } else {
            view.create(bundle.withId(id), user, getLogMessage(logMessage));
        }
    }
}
