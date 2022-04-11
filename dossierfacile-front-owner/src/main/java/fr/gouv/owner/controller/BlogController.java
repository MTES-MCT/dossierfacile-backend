package fr.gouv.owner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BlogController {

    @GetMapping("/blog")
    public String index() {
        return "blog/index";
    }

    @GetMapping("/blog/article1")
    public String article1() {
        return "blog/article1";
    }

    @GetMapping("/blog/article2")
    public String article2() {
        return "blog/article2";
    }

    @GetMapping("/blog/article3")
    public String article3() {
        return "blog/article3";
    }
}
