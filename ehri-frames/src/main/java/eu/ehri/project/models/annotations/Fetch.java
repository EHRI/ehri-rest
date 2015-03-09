package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * <pre>
 *   public class MyEntity {
 *      &064;Fetch
 *      &064;Adjacency(label="hasDate")
 *      public Iterator<DatePeriod> getDates();
 *   }
 * </pre>
 * 
 * Marks the entity pointed to by the framed Adjacency as one that should
 * typically be fetched and displayed along with the master object.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fetch {
    public static final int DEFAULT_TRAVERSALS = 15;

    /**
     * Default number of traversals to make on @Fetch'ed relations.
     * 
     * @return the depth to which this relationship should be traversed
     */
    int depth() default DEFAULT_TRAVERSALS;

    /**
     * Only traverse this relationship at the specified depth. Ignored
     * if the depth is < 0.
     *
     * @return the depth at which this relation should be traversed
     */
    int ifDepth() default -1;

    /**
     * Only traverse this relationship when not doing 'lite' serialization,
     * e.g. when we want maximum detail.
     *
     * @return boolean switch
     */
    boolean whenNotLite() default false;

    /**
     * Override lite/full serialization modes and always serialize
     * this relationship.
     *
     * @return boolean switch
     */
    boolean full() default false;

    /**
     * The key name of the serialized relationship.
     *
     * @return the name used in output data
     */
    String value();
}
