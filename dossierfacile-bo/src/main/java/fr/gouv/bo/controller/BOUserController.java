package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.model.RoleDTO;
import fr.gouv.bo.security.RoleService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.UserService;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BOUserController {
    private static final String EMAIL = "email";
    private final UserService userService;
    private final RoleService roleComparator;

    @GetMapping("/bo/users")
    public String getBOUser(Model model, @AuthenticationPrincipal UserPrincipal principal) {

        var highestRole = roleComparator.getHighestRole(principal.getAuthorities());
        var availableRolesDto = roleComparator.getAvailableRoles(highestRole);
        model.addAttribute(EMAIL, new EmailDTO());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("availableRolesDto", availableRolesDto);
        model.addAttribute("availableRoles", availableRolesDto.stream().map(RoleDTO::value).toList());
        return "bo/users";
    }

    @PostMapping("/bo/users")
    public String createBOUser(
            EmailDTO emailDTO,
            Model model,
            @RequestParam(defaultValue = "ROLE_OPERATOR", name = "action") Role role,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BOUser user = userService.findUserByEmail(emailDTO.getEmail());
        var highestRole = roleComparator.getHighestRole(principal.getAuthorities());
        if (highestRole == null || !roleComparator.isRoleGreaterOrEqual(highestRole, role)) {
            throw new ForbiddenException("Vous n'avez pas les droits suffisants pour effectuer cette action");
        }
        if (user == null) {
            userService.createUserByEmail(emailDTO.getEmail(), role);
        } else {
            throw new IllegalArgumentException("Utilisateur existe déjà");
        }

        return "redirect:/bo/users";
    }

    @DeleteMapping("/bo/users/{email}/roles/{role}")
    public String deleteRole(
            Model model,
            @PathVariable(name = "email") String email,
            @PathVariable(name = "role") Role role,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        var highestRole = roleComparator.getHighestRole(principal.getAuthorities());
        if (highestRole == null || !roleComparator.isRoleGreaterOrEqual(highestRole, role)) {
            throw new ForbiddenException("Vous n'avez pas les droits suffisants pour effectuer cette action");
        }
        BOUser user = userService.findUserByEmail(email);
        userService.deleteRoles(user, Collections.singletonList(role));
        return "redirect:/bo/users";
    }

    @PostMapping("/bo/users/{email}/roles")
    public String addRole(
            @RequestParam(name = "role") Role role,
            Model model,
            @PathVariable(name = "email") String email,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BOUser user = userService.findUserByEmail(email);
        var highestRole = roleComparator.getHighestRole(principal.getAuthorities());
        if (highestRole == null || !roleComparator.isRoleGreaterOrEqual(highestRole, role)) {
            throw new ForbiddenException("Vous n'avez pas les droits suffisants pour effectuer cette action");
        }
        userService.addRoles(user, Collections.singletonList(role));
        return "redirect:/bo/users";
    }
}
