package fr.dossierfacile.dfcdemo.web;

import fr.dossierfacile.dfcdemo.oidc.DfcApiException;
import fr.dossierfacile.dfcdemo.oidc.DfcConnectService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DfcConnectController {

    /** The access token stays server side; the original demo passed it in URLs, which leaks it in logs and history. */
    static final String SESSION_ACCESS_TOKEN = "DFC_ACCESS_TOKEN";

    private final DfcConnectService dfcConnectService;

    @GetMapping("/connection-dfc")
    public String connection(Model model) {
        model.addAttribute("authorizeUrl", dfcConnectService.authorizeUrl());
        return "connection-dfc";
    }

    @GetMapping("/display")
    public String display(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpSession session,
            Model model) {
        if (error != null) {
            // Keycloak redirects here with an error instead of a code, e.g. when the tenant refuses the consent
            throw new DfcApiException(DfcConnectService.STEP_AUTHORIZE, null, error + ": " + errorDescription);
        }
        if (code == null || code.isBlank()) {
            throw new DfcApiException(DfcConnectService.STEP_AUTHORIZE, null, "missing authorization code");
        }

        String accessToken = dfcConnectService.exchangeCode(code);
        session.setAttribute(SESSION_ACCESS_TOKEN, accessToken);

        model.addAttribute("redirectUri", dfcConnectService.redirectUri());
        model.addAttribute("tokenUrl", dfcConnectService.tokenUrl());
        model.addAttribute("profileUrl", dfcConnectService.profileUrl());
        model.addAttribute("profileJson", dfcConnectService.fetchProfile(accessToken));
        return "display";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String accessToken = (String) session.getAttribute(SESSION_ACCESS_TOKEN);
        if (accessToken == null) {
            return "redirect:/";
        }
        try {
            dfcConnectService.logout(accessToken);
        } finally {
            session.invalidate();
        }
        return "logout";
    }
}
