package fr.gouv.owner.controller;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;

@Controller
@Profile({"prod", "preprod"})
public class LocatioErrorController implements ErrorController {

    private final SecureRandom rand = new SecureRandom ();

    @GetMapping(value = "/error", produces = "text/html")
    public String handleError() {
        int i = rand.nextInt((3)) + 1;
        return "error/error" + i;
    }

    @GetMapping(value = "/error", produces = "application/json")
    public ResponseEntity<HttpStatus> handleError2(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            for (HttpStatus httpStatus : HttpStatus.values()) {
                if (httpStatus.value() == statusCode) {
                    return new ResponseEntity<>(httpStatus);
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}