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

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.Transaction;

abstract public class AbstractFixtureTest extends ModelTestBase {

    // Members closely coupled to the test data!
    protected UserProfile validUser;
    protected UserProfile invalidUser;
    protected DocumentaryUnit item;
    protected Transaction tx;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        try (Transaction tx = service.beginTx()) {
            item = manager.getEntity("c1", DocumentaryUnit.class);
            validUser = manager.getEntity("mike", UserProfile.class);
            invalidUser = manager.getEntity("reto", UserProfile.class);
            tx.commit();
        } catch (ItemNotFound e) {
            throw new RuntimeException(e);
        }

        tx = service.beginTx();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        tx.rollback();
        tx.close();
        super.tearDown();
    }
}
