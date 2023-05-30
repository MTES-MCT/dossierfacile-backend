package fr.dossierfacile.api.dossierfacileapiowner.webhook;

import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@AllArgsConstructor
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {
    private final WebhookService webhookService;

    @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> webhook(@RequestBody ApplicationModel applicationModel, HttpServletResponse response) {
        log.info(applicationModel.toString());
        webhookService.handleWebhook(applicationModel);
        return ResponseEntity.ok().build();
    }
}
