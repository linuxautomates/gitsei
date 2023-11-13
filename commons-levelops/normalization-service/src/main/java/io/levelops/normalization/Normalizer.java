package io.levelops.normalization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate methods that can be used to normalize data of a given {@link io.levelops.commons.models.ContentType}.
 * <p>
 * Methods must be <b>static</b> with the following signature:
 * <p>
 * {@code public static Output method(ObjectMapper objectMapper, Input input)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Normalizer {
    String contentType();
}
