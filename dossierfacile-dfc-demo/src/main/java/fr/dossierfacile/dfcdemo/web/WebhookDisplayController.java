package fr.dossierfacile.dfcdemo.web;

import fr.dossierfacile.dfcdemo.webhook.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebhookDisplayController {

    private static final int DISPLAYED_ROWS = 50;

    private final WebhookLogRepository repository;

    @GetMapping("/display-webhook")
    public String list(Model model) {
        model.addAttribute("webhooks", repository.findLatest(DISPLAYED_ROWS));
        return "display-webhook";
    }
}
