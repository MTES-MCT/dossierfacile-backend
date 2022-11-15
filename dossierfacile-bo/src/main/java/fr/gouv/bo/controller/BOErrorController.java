package fr.gouv.bo.controller;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.SecureRandom;

@Controller
@Profile("preprod")
public class BOErrorController implements ErrorController {

    private final SecureRandom rand = new SecureRandom ();

    @GetMapping(value = "/error", produces = "text/html")
    public String handleError() {
        int i = rand.nextInt((3)) + 1;
        return "error/error" + i;
    }

}