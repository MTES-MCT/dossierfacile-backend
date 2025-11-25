package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.model.documentIA.WebhookModel;
import fr.dossierfacile.api.front.service.interfaces.DocumentIAService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/webhook")
public class DocumentIAController {

    private final DocumentIAService documentIAService;

    @PostMapping("/v1/document-ia")
    public ResponseEntity<Void> receiveDocumentIAWebhook(
            HttpServletRequest request,
            @Valid @RequestBody WebhookModel body
    ) {

        documentIAService.handleWebhookCallback(body);

        return ResponseEntity.accepted().build();
    }
}
