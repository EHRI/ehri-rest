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

package eu.ehri.project.api;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.events.SystemEvent;

import java.util.List;

public interface EventsApi {
    Iterable<SystemEvent> list();

    Iterable<List<SystemEvent>> aggregate();

    Iterable<SystemEvent> listAsUser(UserProfile asUser);

    Iterable<List<SystemEvent>> aggregateAsUser(UserProfile asUser);

    Iterable<SystemEvent> listForItem(Accessible item);

    Iterable<List<SystemEvent>> aggregateForItem(Accessible item);

    Iterable<List<SystemEvent>> aggregateActions(Actioner byUser);

    Iterable<SystemEvent> listByUser(UserProfile byUser);

    EventsApi from(String from);

    EventsApi to(String to);

    EventsApi withIds(String... ids);

    EventsApi withRange(int offset, int limit);

    EventsApi withUsers(String... users);

    EventsApi withEntityClasses(EntityClass... entityTypes);

    EventsApi withEventTypes(EventTypes... eventTypes);

    EventsApi withShowType(ShowType... type);

    EventsApi withAggregation(Aggregation aggregation);

    // Discriminator for personalised events
    enum ShowType {
        watched, followed
    }

    // Discriminator for aggregation type
    enum Aggregation {
        user, strict, off
    }
}
