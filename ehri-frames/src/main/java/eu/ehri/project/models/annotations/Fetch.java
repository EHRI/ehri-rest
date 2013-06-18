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
    public static final int DEFAULT_TRAVERSALS = 5;

    /**
     * Default number of traversals to make on @Fetch'ed relations.
     * 
     * @return
     */
    int depth() default DEFAULT_TRAVERSALS;

    int ifDepth() default -1;

    String value();

}
