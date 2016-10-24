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

package eu.ehri.project.tools;

import com.google.common.collect.Iterables;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LinkerTest extends AbstractFixtureTest {

    private Linker linker;
    private ActionManager actionManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        linker = new Linker(graph);
        actionManager = new ActionManager(graph);
    }

    @Test
    public void testCreateConceptsForRepository() throws Exception {
        Repository repository = manager.getEntity("r1", Repository.class);
        Vocabulary vocabulary = manager.getEntity("cvoc2", Vocabulary.class);
        int eventCount = Iterables.size(actionManager.getLatestGlobalEvents());
        // The doc c1 contains two access point nodes which should be made
        // into links
        String logMessage = "This is a test!";
        int newLinkCount = linker
                .withExcludeSingles(false)
                .withLogMessage(logMessage)
                .createAndLinkRepositoryVocabulary(repository, vocabulary, validUser);
        assertEquals(2, newLinkCount);
        assertEquals(eventCount + 2,
                Iterables.size(actionManager.getLatestGlobalEvents()));
        assertEquals(actionManager.getLatestGlobalEvent().getLogMessage(), logMessage);
    }

    @Test
    public void testCreateConceptsForRepositoryWithBadAccessPointTypes() throws Exception {
        Repository repository = manager.getEntity("r1", Repository.class);
        Vocabulary vocabulary = manager.getEntity("cvoc2", Vocabulary.class);
        int eventCount = Iterables.size(actionManager.getLatestGlobalEvents());

        // This won't create any links because the access point type
        // won't match any existing access points.
        int newLinkCount = linker
                .withExcludeSingles(false)
                .withAccessPointType(AccessPointType.genre)
                .createAndLinkRepositoryVocabulary(repository, vocabulary, validUser);
        assertEquals(0, newLinkCount);
        // No new events should have been created...
        assertEquals(eventCount,
                Iterables.size(actionManager.getLatestGlobalEvents()));
    }

    @Test
    public void testCreateConceptsForRepositoryExcludingSingles() throws Exception {
        Repository repository = manager.getEntity("r1", Repository.class);
        Vocabulary vocabulary = manager.getEntity("cvoc2", Vocabulary.class);
        int eventCount = Iterables.size(actionManager.getLatestGlobalEvents());

        // This won't create any links because all of the access points only point
        // to single items
        int newLinkCount = linker.withExcludeSingles(true)
            .createAndLinkRepositoryVocabulary(repository, vocabulary,
                    validUser);
        assertEquals(0, newLinkCount);
        // No new events should have been created...
        assertEquals(eventCount,
                Iterables.size(actionManager.getLatestGlobalEvents()));
    }
}
