package fr.dossierfacile.dossierfacilefileanalysis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestFunction {

    @PostConstruct
    public void init() {
        try {
            log.info("Loading OpenCV library");
            System.load("/usr/lib/jni/libopencv_java460.so");
        } catch (Exception e) {
            log.error("Error loading OpenCV library ", e);
        }
        process();
    }

    public void process() {
        log.info("Processing test function");
    }

}
