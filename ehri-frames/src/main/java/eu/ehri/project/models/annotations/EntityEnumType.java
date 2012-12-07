package eu.ehri.project.models.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import eu.ehri.project.models.EntityEnumTypes;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntityEnumType {
    public static final String TYPE_KEY = "__ISA__";
    public static final String ID_KEY = "__ID__";

    public EntityEnumTypes value();
}
