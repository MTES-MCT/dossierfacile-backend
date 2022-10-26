package fr.dossierfacile.api.pdf.controller;

import fr.dossierfacile.api.pdf.service.interfaces.FeedbackService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    @GetMapping(value = "/{value}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void feedback(@PathVariable("value") boolean value) {
        feedbackService.saveFeedback(value);
    }
}
