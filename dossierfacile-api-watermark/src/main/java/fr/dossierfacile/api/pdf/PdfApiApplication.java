package fr.dossierfacile.api.pdf;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@EnableJpaAuditing
@ComponentScan(basePackages = "fr.dossierfacile")
@EnableScheduling
@EnableAsync
@Slf4j
public class PdfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfApiApplication.class, args);
        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
        log.info("Initial Memory (xms) : {}mb", xms);
        log.info("Max Memory (xmx) : {}mb", xmx);
    }

}
