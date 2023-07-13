package fr.gouv.bo.controller;


import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class BOErrorController implements ErrorController {

    @GetMapping(value = "/error", produces = "text/html")
    public String handleError(Model model, HttpServletRequest request) {
        model.addAttribute("statusCode", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        model.addAttribute("statusMessage", request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        return "error/error";
    }
}