/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.test;

import com.google.common.collect.Lists;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.Transaction;

import java.util.List;


public abstract class ModelTestBase extends GraphTestBase {

    protected FixtureLoader helper;

    protected <T> List<T> toList(Iterable<T> iter) {
        return Lists.newArrayList(iter);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper = FixtureLoaderFactory.getInstance(graph);
        try (Transaction tx = service.beginTx()) {
            helper.loadTestData();
            tx.commit();
        }
    }
}
