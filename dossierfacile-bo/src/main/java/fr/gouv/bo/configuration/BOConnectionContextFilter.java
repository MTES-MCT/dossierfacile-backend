package fr.gouv.bo.configuration;

import fr.dossierfacile.common.config.AbstractConnectionContextFilter;
import fr.gouv.bo.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public class BOConnectionContextFilter extends AbstractConnectionContextFilter {

    private static final String EMAIL = "email";

    @Override
    public boolean isRequestIgnored(HttpServletRequest request) {
        return request.getRequestURI().matches("(/assets/|/js/|/css/|/fonts/|/webjars/:?).*");
    }

    @Override
    public Map<String, String> getAdditionalContextElements() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.getEmail() != null) {
            return Map.of(EMAIL, principal.getEmail());
        }
        return Map.of();
    }

}