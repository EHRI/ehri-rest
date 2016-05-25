package eu.ehri.project.views.api;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Entity;

import java.util.Collection;
import java.util.Iterator;


public interface QueryApi {
    int DEFAULT_LIMIT = 20;

    /**
     * Set the offset of this query.
     *
     * @param offset the number of items to skip
     * @return a new query
     */
    QueryApi setOffset(int offset);

    /**
     * Set the limit of this query.
     *
     * @param limit the limit of the number of items returned
     * @return a new query
     */
    QueryApi setLimit(int limit);

    /**
     * Add a filter predicate.
     *
     * @param key       a filter key
     * @param predicate a predicate
     * @param value     the filter predicate
     * @return a new query with the filter added
     */
    QueryApi filter(String key, FilterPredicate predicate, Object value);

    /**
     * Set all filters for this query, parsing the key/predicate/value
     * from string values.
     *
     * @param filterSpecs a set of filter spec strings
     * @return a new query with previous filters replaced
     */
    QueryApi filter(Collection<String> filterSpecs);

    /**
     * Set the query result order.
     *
     * @param key   a order key
     * @param order an order spec
     * @return a new query with the ordering added
     */
    QueryApi orderBy(String key, Sort order);

    /**
     * Set all orderings for this query, parsing the key/spec
     * from string values.
     *
     * @param orderSpecs a set of order spec strings
     * @return a new query with previous filters replaced
     */
    QueryApi orderBy(Collection<String> orderSpecs);

    /**
     * Toggle streaming, which will disable paging totals,
     * assuming an infinite collection.
     *
     * @param stream whether to assume streaming
     * @return a new query with streaming enable/disabled
     */
    QueryApi setStream(boolean stream);

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    <E extends Entity> Page<E> page(Class<E> cls);

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    <E extends Entity> Page<E> page(EntityClass type, Class<E> cls);

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    <E extends Entity> Page<E> page(Iterable<E> vertices, Class<E> cls);

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    <E extends Entity> Page<E> page(String key, String query, Class<E> cls);

    /**
     * Count items accessible to a given user.
     */
    long count(Iterable<?> vertices);

    /**
     * Count all items of a given type.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    long count(EntityClass type);

    /**
     * Directions for sort.
     */
    enum Sort {
        ASC, DESC
    }

    /**
     * Filter predicates
     */
    enum FilterPredicate {
        EQUALS, IEQUALS, STARTSWITH, ENDSWITH, CONTAINS, ICONTAINS, MATCHES, GT, GTE, LT, LTE
    }

    /**
     * Class representing a page of content.
     *
     * @param <T> the item type
     */
    class Page<T> implements Iterable<T> {
        private final Iterable<T> iterable;
        private final int page;
        private final int count;
        private final long total;

        public Page(Iterable<T> iterable, int page, int count, long total) {
            this.iterable = iterable;
            this.total = total;
            this.page = page;
            this.count = count;
        }

        public Iterable<T> getIterable() {
            return iterable;
        }

        public long getTotal() {
            return total;
        }

        public Integer getOffset() {
            return page;
        }

        public Integer getLimit() {
            return count;
        }

        @Override
        public Iterator<T> iterator() {
            return iterable.iterator();
        }

        @Override
        public String toString() {
            return String.format("<Page[...] %d %d (%d)", page, count, total);
        }
    }
}
