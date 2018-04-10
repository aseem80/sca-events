package io.nordstrom.org.scaevents.annotation;

import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by bmwi on 4/9/18.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncRetryable {

    /**
     * Retry interceptor bean name to be applied for retryable method. Is mutually
     * exclusive with other attributes.
     * @return the retry interceptor bean name
     */
    String interceptor() default "";

    /**
     * Exception types that are retryable. Synonym for includes(). Defaults to empty (and
     * if excludes is also empty all exceptions are retried).
     * @return exception types to retry
     */
    Class<? extends Throwable>[] value() default {};



    /**
     * @return the maximum number of attempts (including the first failure), defaults to 3
     */
    int maxAttempts() default 3;



    /**
     * Specify the backoff properties for retrying this operation. The default is no
     * backoff, but it can be a good idea to pause between attempts (even at the cost of
     * blocking a thread).
     * @return a backoff specification
     */
    Backoff backoff() default @Backoff();

}
