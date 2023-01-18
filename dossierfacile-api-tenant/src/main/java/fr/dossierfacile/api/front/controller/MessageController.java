package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.MessageForm;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.MessageService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/message", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final AuthenticationFacade authenticationFacade;

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @GetMapping("")
    public ResponseEntity<List<MessageModel>> all(@RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(messageService.findAll(authenticationFacade.getTenant(tenantId)));
    }

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @PutMapping("/read")
    public ResponseEntity<List<MessageModel>> markAsRead(@RequestParam(required = false) Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        List<MessageModel> messages = messageService.markMessagesAsRead(tenant);
        return ResponseEntity.ok(messages);
    }

    @PreAuthorize("hasPermissionOnTenant(#messageForm.tenantId)")
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageModel> create(@Validated @RequestBody MessageForm messageForm) {
        var tenant = authenticationFacade.getTenant(messageForm.getTenantId());
        messageService.updateStatusOfDeniedDocuments(tenant);
        return ResponseEntity.ok(messageService.create(tenant, messageForm, false));
    }
}
