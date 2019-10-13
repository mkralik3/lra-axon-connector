package org.mkralik.learning.lra.axon.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Field annotated by this annotation is used to specify the LRA context explicitly by the user.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LRAContext {
}
