package org.elshift.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides information about a command parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
    /**
     * @return The option's name
     */
    String name();

    /**
     * @return The option's description
     */
    String description() default "";

    /**
     * @return Whether this option is required
     */
    boolean required() default true;
}