package eu.ehri.project.persistence;

import com.google.common.collect.Multimap;

import java.util.List;

/**
 * A data structure representation of a property graph.
 *
 * @param <N> the concrete type
 */
public interface NestableData<N> {

    Multimap<String, N> getRelations();

    List<N> getRelations(String relation);

    boolean hasRelations(String relation);

    <T> T getDataValue(String key);

    N withRelations(Multimap<String, N> relations);

    N withRelations(String name, List<N> relations);

    N withRelation(String name, N value);

    N replaceRelations(Multimap<String, N> relations);

    N withDataValue(String key, Object value);

    N removeDataValue(String key);
}
