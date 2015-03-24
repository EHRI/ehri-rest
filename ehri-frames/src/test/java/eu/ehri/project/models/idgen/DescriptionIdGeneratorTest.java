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

package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DescriptionIdGeneratorTest extends AbstractFixtureTest {

    private IdGenerator instance;
    private List<String> scopes;
    private Bundle bundle;

    @Override
    @Before
    public void setUp() throws Exception{
        super.setUp();
        instance = DescriptionIdGenerator.INSTANCE;
        scopes = Lists.newArrayList("nl", "r1", "c1");
        bundle = Bundle.fromData(TestData.getTestDocBundle())
                .getRelations(Ontology.DESCRIPTION_FOR_ENTITY).get(0);
    }

    @Test()
    public void testHandleIdCollision() throws Exception {
        ListMultimap<String,String> errors = instance.handleIdCollision(scopes, bundle);
        assertTrue(errors.containsKey(Ontology.LANGUAGE));
    }

    @Test
    public void testGenerateIdWithStringScopes() throws Exception {
        String id = instance.generateId(scopes, bundle);
        assertEquals("nl-r1-c1.eng-someid_01", id);

    }

    @Test
    public void testGetIdBase() throws Exception {
        String id = instance.getIdBase(bundle);
        assertEquals("eng-someid_01", id);
    }
}
