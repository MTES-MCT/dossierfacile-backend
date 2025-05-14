package fr.dossierfacile.process.file.configuration;

import co.elastic.apm.attach.ElasticApmAttacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "elastic.apm", name = "enabled", havingValue = "true")
public class ElasticApmConfiguration {

    @PostConstruct
    public void init() {
        log.info("Start ElasticApmAttacher");
        ElasticApmAttacher.attach();
    }

}
