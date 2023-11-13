package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.service.LogService;
import fr.gouv.bo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping(value = "/bo/dashboard")
public class BODashboardController {
    public final String EMAIL = "email";
    @Autowired
    private LogService logService;
    @Autowired
    private UserService userService;

    @GetMapping("")
    public String myDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        BOUser operator = userService.findUserByEmail(principal.getName());
        List<Object[]> listTreatedCountByDay = logService.listLastTreatedFilesByOperator(operator.getId());

        model.addAttribute("operator", operator);
        model.addAttribute("listTreatedCountByDay", listTreatedCountByDay);

        model.addAttribute(EMAIL, new EmailDTO());
        return "bo/dashboard";
    }
}
