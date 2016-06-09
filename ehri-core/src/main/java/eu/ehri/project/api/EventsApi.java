package eu.ehri.project.api;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.events.SystemEvent;

import java.util.List;

public interface EventsApi {
    Iterable<SystemEvent> list();

    Iterable<List<SystemEvent>> aggregate();

    Iterable<SystemEvent> listAsUser(UserProfile asUser);

    Iterable<List<SystemEvent>> aggregateAsUser(UserProfile asUser);

    Iterable<SystemEvent> listForItem(Accessible item);

    Iterable<List<SystemEvent>> aggregateForItem(Accessible item);

    Iterable<List<SystemEvent>> aggregateUserActions(UserProfile byUser);

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
