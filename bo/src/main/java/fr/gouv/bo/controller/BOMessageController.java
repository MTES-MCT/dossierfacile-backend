package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.service.MessageService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping(value = "/bo/message")
public class BOMessageController {

    private static final String APARTMENT_SHARING_ID = "aptSharingId";
    private static final String MESSAGES = "messages";
    private static final String TENANT = "tenant";
    private static final String NAME = "name";

    private final TenantService tenantService;
    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/tenant/{id}")
    public String tenantMessages(Model model, @PathVariable("id") Long id, Principal principal) {

        User tenant = tenantService.getUserById(id);
        Tenant tenant1 = tenantService.getTenantById(tenant.getId());
        ApartmentSharing apartmentSharing = tenant1.getApartmentSharing();
        User loggedUser = userService.findUserByEmail(principal.getName());
        List<Message> messages = messageService.findTenantMessages(tenant);

        messageService.markReadAdmin(tenant);

        model.addAttribute("message", MessageDTO.builder().message("Bonjour,\n\n" +
                "Merci pour votre message.\n\n\n" +
                "En vous souhaitant une très bonne journée,\n" +
                "Marie pour l’équipe DossierFacile").build());
        model.addAttribute(APARTMENT_SHARING_ID, apartmentSharing.getId());
        model.addAttribute(MESSAGES, messages);
        model.addAttribute(TENANT, tenant);
        model.addAttribute(NAME, loggedUser.getFullName());
        return "bo/message";
    }

    @PostMapping("/new/{id}")
    public String newMessage(MessageDTO messageDTO, @PathVariable("id") Long tenantId, Principal principal) {
        return tenantService.updateStatusOfTenantFromAdmin(principal, messageDTO, tenantId);
    }

    @PostMapping("/mistake/{id}")
    public String mistakeMessage(@PathVariable Long id, Principal principal) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant == null) {
            return "redirect:/error";
        }
        messageService.createMistakeMessage(tenant);
        tenantService.validateTenantFile(principal, tenant.getId());
        return "redirect:/bo";
    }
}

