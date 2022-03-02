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

package eu.ehri.project.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;

import java.util.Map;

public class TestData {
    public static final String TEST_COLLECTION_NAME = "A brand new collection";
    public static final String TEST_START_DATE = "1945-01-01T00:00:00Z";
    public static final String TEST_USER_NAME = "Joe Blogs";
    public static final String TEST_GROUP_NAME = "People";


    public static Map<String, Object> getTestDocBundle() {
        // Data structure representing a not-yet-created collection.
        return ImmutableMap.<String, Object>of(
                Bundle.TYPE_KEY, Entities.DOCUMENTARY_UNIT,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.NAME_KEY, TEST_COLLECTION_NAME,
                        Ontology.IDENTIFIER_KEY, "someid-01"
                ),
                Bundle.REL_KEY, ImmutableMap.of(
                        Ontology.DESCRIPTION_FOR_ENTITY, ImmutableList.of(
                                ImmutableMap.of(
                                        Bundle.TYPE_KEY, Entities.DOCUMENTARY_UNIT_DESCRIPTION,
                                        Bundle.DATA_KEY, ImmutableMap.of(
                                                Ontology.IDENTIFIER_KEY, "someid-01",
                                                Ontology.NAME_KEY, "A brand new item description",
                                                Ontology.LANGUAGE_OF_DESCRIPTION, "eng"
                                        ),
                                        Bundle.REL_KEY, ImmutableMap.of(
                                                Ontology.ENTITY_HAS_DATE, ImmutableList.of(
                                                        ImmutableMap.of(
                                                                Bundle.TYPE_KEY, Entities.DATE_PERIOD,
                                                                Bundle.DATA_KEY, ImmutableMap.of(
                                                                        Ontology.DATE_PERIOD_START_DATE, TEST_START_DATE,
                                                                        Ontology.DATE_PERIOD_END_DATE, TEST_START_DATE
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    public static Map<String, Object> getTestAgentBundle() {
        // Data structure representing a not-yet-created collection.
        return ImmutableMap.<String, Object>of(
                Bundle.TYPE_KEY, Entities.REPOSITORY,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.NAME_KEY, "Test Repo 1",
                        Ontology.IDENTIFIER_KEY, "test-repo-1"
                ),
                Bundle.REL_KEY, ImmutableMap.of(
                        Ontology.DESCRIPTION_FOR_ENTITY, ImmutableList.of(
                                ImmutableMap.of(
                                        Bundle.TYPE_KEY, Entities.REPOSITORY_DESCRIPTION,
                                        Bundle.DATA_KEY, ImmutableMap.of(
                                                Ontology.IDENTIFIER_KEY, "test-repo-1-desc",
                                                Ontology.NAME_KEY, "A Test Repository",
                                                Ontology.LANGUAGE_OF_DESCRIPTION, "eng"
                                        ),
                                        Bundle.REL_KEY, ImmutableMap.of(
                                                Ontology.ENTITY_HAS_ADDRESS, ImmutableList.of(
                                                        ImmutableMap.of(
                                                                Bundle.TYPE_KEY, Entities.ADDRESS,
                                                                Bundle.DATA_KEY, ImmutableMap.of("name", "primary")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    public static Map<String, Object> getTestUserBundle() {
        // Data structure representing a not-yet-created user.
        return ImmutableMap.<String, Object>of(
                Bundle.TYPE_KEY, Entities.USER_PROFILE,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.NAME_KEY, TEST_USER_NAME,
                        Ontology.IDENTIFIER_KEY, "joe-blogs"
                )
        );
    }

    public static Map<String, Object> getTestGroupBundle() {
        // Data structure representing a not-yet-created group.
        return ImmutableMap.of(
                Bundle.TYPE_KEY, Entities.GROUP,
                Bundle.DATA_KEY, ImmutableMap.of(
                        Ontology.NAME_KEY, TEST_GROUP_NAME,
                        Ontology.IDENTIFIER_KEY, "people"
                )
        );
    }
}
