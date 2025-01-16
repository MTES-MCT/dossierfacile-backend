package fr.dossierfacile.api.front.log;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class LogConfiguration {

    @PostConstruct
    public void setupCustomLogging() {
        CustomAppender.attachToRootLogger();
    }
}