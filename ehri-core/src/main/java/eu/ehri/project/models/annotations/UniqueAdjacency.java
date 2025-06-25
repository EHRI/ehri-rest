package eu.ehri.project.models.annotations;

import com.tinkerpop.blueprints.Direction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When annotating a setter method adds an adjacency
 * between source and target if it does not already exist.
 *
 * On a getter method retrieves the target.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UniqueAdjacency {
    /**
     * The label of the edges making the adjacency between the vertices.
     *
     * @return the edge label
     */
    String label();

    /**
     * The edge direction of the adjacency.
     *
     * @return the direction of the edges composing the adjacency
     */
    Direction direction() default Direction.OUT;

    /**
     * If true, specifies that the source can only have one
     * relationship of this type.
     *
     * @return the direction of the edges composing the adjacency
     */
    boolean single() default false;
}
