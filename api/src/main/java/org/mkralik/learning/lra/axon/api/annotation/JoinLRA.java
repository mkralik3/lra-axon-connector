package org.mkralik.learning.lra.axon.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotation marks command class which is going to the aggregate which wants to join to the LRA.
 * </p>
 * <p>
 * When the command is sent from the JAX-RS method where some lra context exist, the lra context is automatically added
 * to the command's metadata. The user can also provides own lra context by annotated field by {@link org.mkralik.learning.lra.axon.api.annotation.LRAContext} annotation.
 * </p>
 * <p>
 * At least one lra context needs to be available when the command is annotated by this annotation. When the LRA Context is arrived from
 * the JAX-RS method and also user specify LRAContext explicitly by {@link org.mkralik.learning.lra.axon.api.annotation.LRAContext},
 * the LRA context from the field is used {@link org.mkralik.learning.lra.axon.api.annotation.LRAContext}. When no context is available,
 * the {@link javax.naming.ConfigurationException} exception is thrown.
 * </p>
 * <p>
 * The metadata is accessible in the command handler via {@value org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_CONTEXT_HEADER}
 * parameter annotated by {@link org.axonframework.messaging.annotation.MetaDataValue}. The lra context metadata is also
 * added to all events which are applied from the constructor where the command goes.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface JoinLRA {
}