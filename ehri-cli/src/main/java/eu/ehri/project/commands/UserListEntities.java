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
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.api.QueryApi;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * List items of a specific type as a given user, respecting access controls.
 */
public class UserListEntities extends BaseCommand {

    final static String NAME = "user-list";

    @Override
    public String getUsage() {
        return String.format("%s [OPTIONS] <type>", NAME);
    }

    @Override
    public String getHelp() {
        return "List entities of a given type as a given user.";
    }

    @Override
    public void setCustomOptions(Options options) {
        options.addOption(Option.builder()
                .longOpt("user")
                .hasArg()
                .type(String.class)
                .hasArg().desc("Identifier of user to list items as")
                .build());
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph,
            CommandLine cmdLine) throws Exception {

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getUsage());

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        EntityClass type = EntityClass.withName(cmdLine.getArgs()[0]);
        Class<?> cls = type.getJavaClass();

        if (!Accessible.class.isAssignableFrom(cls))
            throw new RuntimeException("Unknown accessible entity: " + type);

        UserProfile user = manager.getEntity(
                cmdLine.getOptionValue("user"), UserProfile.class);

        @SuppressWarnings("unchecked")
        QueryApi query = api(graph, user).query();
        for (Entity acc : query.page(Entity.class)) {
            System.out.println(acc.getId());
        }

        return 0;
    }
}
