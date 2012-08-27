package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * 
 * <pre>
 *   public class MyEntity {
 *      &064;CascadeDelete
 *      &064;Fetch
 *      &064;Adjacency(label="hasDate")
 *      public Iterator<DatePeriod> getDates();
 *   }
 * </pre>
 * 
 * Indicates that the entity pointed to by the Framed Adjacency
 * should be deleted along with its master object.
 */
public @interface CascadeDelete {

}
