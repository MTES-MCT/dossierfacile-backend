package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.form.MessageForm;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.MessageService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api-partner/tenant/{tenantId}/message", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiPartnerMessageController {
    private final MessageService messageService;
    private final AuthenticationFacade authenticationFacade;

    @GetMapping("")
    public ResponseEntity<List<MessageModel>> all(@PathVariable Long tenantId) {
        return ResponseEntity.ok(messageService.findAll(authenticationFacade.getTenant(tenantId)));
    }

    @PutMapping("/read")
    public ResponseEntity<List<MessageModel>> markAsRead(@PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        List<MessageModel> messages = messageService.markMessagesAsRead(tenant);
        return ResponseEntity.ok(messages);
    }

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageModel> create(@Validated @RequestBody MessageForm messageForm, @PathVariable Long tenantId) {
        var tenant = authenticationFacade.getTenant(tenantId);
        messageService.updateStatusOfDeniedDocuments(tenant);
        return ResponseEntity.ok(messageService.create(tenant, messageForm, false));
    }
}
