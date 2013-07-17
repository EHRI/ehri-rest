package eu.ehri.project.models.annotations;

import eu.ehri.project.models.EntityClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntityType {
    public static final String TYPE_KEY = "__ISA__";
    public static final String ID_KEY = "__ID__";

    public EntityClass value();
}
