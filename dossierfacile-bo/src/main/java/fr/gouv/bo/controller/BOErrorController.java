package fr.gouv.bo.controller;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BOErrorController implements ErrorController {
    @GetMapping(value = "/error", produces = "text/html")
    public String handleError() {
        return "error/error";
    }
}