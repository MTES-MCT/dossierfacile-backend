package fr.dossierfacile.logging.async;

import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables Spring's asynchronous method execution ({@code @Async}) with an executor
 * that propagates the logging MDC context to worker threads (see
 * {@link MdcAwareAsyncConfiguration}).
 * <p>
 * Use this in place of {@link EnableAsync} on the application configuration of web
 * modules so that asynchronous logs keep their request_id and are picked up by the
 * log aggregator.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableAsync(proxyTargetClass = true)
@Import(MdcAwareAsyncConfiguration.class)
public @interface EnableMdcAwareAsync {
}
