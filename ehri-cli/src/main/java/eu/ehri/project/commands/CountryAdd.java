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

import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Add a country.
 */
public class CountryAdd extends BaseCommand {

    final static String NAME = "countryadd";

    @Override
    protected void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .type(String.class)
                .desc("Identifier of user to import as")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("name")
                .hasArg()
                .type(String.class)
                .desc("Country's full name")
                .build());
        options.addOption(Option.builder()
                .longOpt("log")
                .hasArg()
                .type(String.class)
                .desc("Log message for create action.")
                .build());
    }

    @Override
    public String getUsage() {
        return String.format("%s <country-identifier> [-n <full country name>] [-c <log comment>]", NAME);
    }

    @Override
    public String getHelp() {
        return "Create a new country";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws ItemNotFound, ValidationError, PermissionDenied, DeserializationError {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        String logMessage = cmdLine.getOptionValue("c",
                "Created via command-line");

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        // Fetch the admin accessor, who's going to do the work.
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER,
                Accessor.class);

        String countryId = cmdLine.getArgList().get(0);
        String countryName = cmdLine.getOptionValue("n", countryId);

        Bundle bundle = new Bundle(EntityClass.COUNTRY,
                Maps.<String, Object>newHashMap())
                .withDataValue(Ontology.IDENTIFIER_KEY, countryId)
                .withDataValue(Ontology.NAME_KEY, countryName);

        String nodeId = EntityClass.COUNTRY.getIdGen().generateId(SystemScope.getInstance().idPath(), bundle);
        bundle = bundle.withId(nodeId);

        try {
            api(graph, admin).create(bundle, Country.class, getLogMessage(logMessage));
        } catch (ValidationError e) {
            System.err.printf("A country with id: '%s' already exists%n", nodeId);
            return 9;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }
}
