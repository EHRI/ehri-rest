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
 *      &064;Mandatory
 *      &064;Property("indentifier")
 *      public String getIdentifier();
 *   }
 * </pre>
 * 
 * Indicates that a property is mandatory.
 */
public @interface Mandatory {

}
