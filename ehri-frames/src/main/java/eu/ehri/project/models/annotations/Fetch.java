package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the entity pointed to by the framed Adjacency as one that should
 * typically be fetched and displayed along with the master object.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fetch {
    public static final int DEFAULT_TRAVERSALS = 10;

    /**
     * Only serialize this relationship if the depth of traversal
     * is below this value. The default value is {@value #DEFAULT_TRAVERSALS}.
     *
     * @return the level below which this relationship should be traversed
     */
    int ifBelowLevel() default DEFAULT_TRAVERSALS;

    /**
     * Only traverse this relationship at the specified level. Ignored
     * if the level is < 0.
     *
     * @return the level at which this relation should be traversed
     */
    int ifLevel() default -1;

    /**
     * The number of levels this relationship should traverse
     * from the current item. i.e. if this item is at level N
     * the serialization of it's relations should stop at level
     * N + {@code Fetch.numLevels()}.
     */
    int numLevels() default -1;

    /**
     * Only serialize this relation when not serializing in
     * 'lite' mode.
     * @return to serialize or not
     */
    boolean whenNotLite() default false;

    /**
     * Always serialize in full mode, regardless of whether
     * lite serialization is enabled.
     *
     * @return override lite mode serialization
     */
    boolean full() default false;

    /**
     * The name of the relation when serialized.
     *
     * @return the relation name
     */
    String value();
}
