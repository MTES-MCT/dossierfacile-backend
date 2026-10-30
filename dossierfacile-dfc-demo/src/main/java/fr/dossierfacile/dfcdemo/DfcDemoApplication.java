package fr.dossierfacile.dfcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Partner demo for DossierFacile Connect. The component scan is deliberately limited to this package: the demo
 * must behave like an external partner and never pick up DossierFacile internals.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DfcDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DfcDemoApplication.class, args);
    }
}
