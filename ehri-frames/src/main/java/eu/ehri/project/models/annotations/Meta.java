package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a Frame method as serializable
 * metadata. This should only be applied on methods that
 * return scalar values.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Meta {
    /**
     * The name of the serialized metadata value.
     *
     * @return a string
     */
    String value();
}
