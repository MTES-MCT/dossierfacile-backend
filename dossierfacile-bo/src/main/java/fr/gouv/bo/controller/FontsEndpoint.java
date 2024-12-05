package fr.gouv.bo.controller;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

@Component
@Endpoint(id = "fonts")
public class FontsEndpoint {

    @ReadOperation
    public List<String> getAvailableFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        return Arrays.asList(fontNames);
    }
}
