package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.form.MessageForm;
import fr.dossierfacile.api.front.model.MessageModel;
import fr.dossierfacile.api.front.service.interfaces.MessageService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final TenantService tenantService;

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @GetMapping("")
    public ResponseEntity<List<MessageModel>> all(@PathVariable Long tenantId) {
        return ResponseEntity.ok(messageService.findAll(tenantService.findById(tenantId)));
    }

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @PutMapping("/read")
    public ResponseEntity<List<MessageModel>> markAsRead(@PathVariable Long tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        List<MessageModel> messages = messageService.markMessagesAsRead(tenant);
        return ResponseEntity.ok(messages);
    }

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageModel> create(@Validated @RequestBody MessageForm messageForm, @PathVariable Long tenantId) {
        var tenant = tenantService.findById(tenantId);
        messageService.updateStatusOfDeniedDocuments(tenant);
        return ResponseEntity.ok(messageService.create(tenant, messageForm, false));
    }
}
