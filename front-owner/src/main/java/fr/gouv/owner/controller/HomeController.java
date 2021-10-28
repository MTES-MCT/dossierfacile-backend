package fr.gouv.owner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/information")
    public String information(@RequestParam(value = "utm_source", required = false) String enterprise) {
        return "information";
    }

    @GetMapping("/securite-des-donnees")
    public String tos() {
        return "tos";
    }
}
