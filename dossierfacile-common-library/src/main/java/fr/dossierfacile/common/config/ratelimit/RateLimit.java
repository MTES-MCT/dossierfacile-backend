package fr.dossierfacile.common.config.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Bucket name / key suffix (e.g., "bo-files", "bo-documents").
     * If left blank, defaults to ClassName.MethodName.
     */
    String name() default "";

    /**
     * Maximum number of requests allowed per minute. (0 means disabled)
     */
    int perMinute() default 0;

    /**
     * Spring property expression for perMinute capacity (e.g. "${ratelimit.bo.admin.files.per.minute:30}")
     */
    String perMinuteString() default "";

    /**
     * Maximum number of requests allowed per day. (0 means disabled)
     */
    int perDay() default 0;

    /**
     * Spring property expression for perDay capacity (e.g. "${ratelimit.bo.admin.files.per.day:100}")
     */
    String perDayString() default "";

    /**
     * Generic capacity limit (used if perMinute and perDay are 0).
     */
    int capacity() default 30;

    /**
     * Period duration for generic capacity.
     */
    long period() default 1;

    /**
     * Time unit for generic period.
     */
    TimeUnit unit() default TimeUnit.MINUTES;
}
