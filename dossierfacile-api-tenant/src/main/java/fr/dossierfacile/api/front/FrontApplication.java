package fr.dossierfacile.api.front;

import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@EnableJpaAuditing
@ServletComponentScan(basePackages = "fr.dossierfacile")
@ComponentScan(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class, basePackages = "fr.dossierfacile", excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {WebFilter.class}))
@EnableScheduling
@EnableCaching
@Slf4j
public class FrontApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontApplication.class, args);
        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
        log.info("Initial Memory (xms) : {}mb", xms);
        log.info("Max Memory (xmx) : {}mb", xmx);
    }

}
