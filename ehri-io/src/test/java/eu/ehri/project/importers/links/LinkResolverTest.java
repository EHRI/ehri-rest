/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.links;

import eu.ehri.project.importers.PreImportCallback;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.idgen.ArkIdGenerator;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinkResolverTest extends AbstractFixtureTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper.setInitializing(false).loadTestData("fixtures/link-resolver.yaml");
    }

    @Test
    public void solveUndeterminedRelationships() throws Exception {
        Described unit = manager.getEntity("c5", Described.class);
        LinkResolver linkResolver = LinkResolver.create(graph, adminUser, PreImportCallback.generatePid(ArkIdGenerator.create(12)));
        int created = linkResolver.solveUndeterminedRelationships(unit);
        assertEquals(2, created);

        // running the same op again shouldn't create more links
        int created2 = linkResolver.solveUndeterminedRelationships(unit);
        assertEquals(0, created2);
    }
}