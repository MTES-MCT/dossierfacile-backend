package fr.dossierfacile.api.pdfgenerator;

import fr.dossierfacile.common.config.ImageIOInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "fr.dossierfacile")
@EntityScan(basePackages = "fr.dossierfacile")
@EnableJpaRepositories(basePackages = "fr.dossierfacile")
@ComponentScan(basePackages = "fr.dossierfacile")
@EnableScheduling
@EnableAsync
@Slf4j
public class PdfGeneratorApplication {

    public static void main(String[] args) {
        ImageIOInitializer.initialize();
        SpringApplication.run(PdfGeneratorApplication.class, args);
        int mb = 1024 * 1024;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
        long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
        log.info("Initial Memory (xms) : {}mb", xms);
        log.info("Max Memory (xmx) : {}mb", xmx);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        log.info(Arrays.stream(fontNames).reduce("", (s, s2) -> s + "\n" + s2));
        String directoryPath = "/app/.fonts";

        // Créez un objet File pour ce répertoire
        File directory = new File(directoryPath);

        // Vérifiez si c'est un répertoire valide
        if (directory.exists() && directory.isDirectory()) {
            // Liste des fichiers et répertoires
            String[] files = directory.list();
            if (files != null) {
                log.info("Contenu de " + directoryPath + " :");
                for (String file : files) {
                    log.info(file);
                }
            } else {
                log.error("Impossible de lire le contenu du répertoire.");
            }
        } else {
            log.error(directoryPath + " n'est pas un répertoire valide.");
        }
    }
}