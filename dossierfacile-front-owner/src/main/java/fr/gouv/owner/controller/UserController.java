package fr.gouv.owner.controller;

import fr.gouv.owner.security.ChangePasswordStatus;
import fr.gouv.owner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/mot-de-passe-oublie")
    public String displayForgotPassword(Model model) {
        return "forgotten-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(String recoveryEmail, Model model) {
        userService.launchPasswordRecoveryProcedure(recoveryEmail);
        return "forgotten-password-success";
    }

    @GetMapping("/changer-de-mot-de-passe/{token}")
    public String displayChangePassword(@PathVariable("token") String token, Model model) {
        model.addAttribute("token", token);
        return "change-password";
    }

    @GetMapping("/creer-un-mot-de-passe/{token}")
    public String createPassword(@PathVariable("token") String token, Model model) {
        model.addAttribute("token", token);
        return "create-password";
    }


    @PostMapping("/change-password")
    public String resetPassword(String token, String password, String passwordConfirmation, Model model) {
        ChangePasswordStatus changePasswordStatus = userService.changePassword(token, password, passwordConfirmation);
        model.addAttribute("status", changePasswordStatus);
        return "change-password-result";
    }

    @GetMapping(path = "/login")
    @PostMapping(path = "/login")
    public String displayLogin() {
        return "login";
    }

}
