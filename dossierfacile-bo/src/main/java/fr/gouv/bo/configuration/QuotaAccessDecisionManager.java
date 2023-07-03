package fr.gouv.bo.configuration;

import fr.gouv.bo.service.QuotaService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

import java.util.Collection;


@AllArgsConstructor
public class QuotaAccessDecisionManager implements AccessDecisionManager {

    private QuotaService quotaService;

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {
        String endpointPath = ((FilterInvocation) object).getRequestUrl();

        if (!quotaService.checkQuota(authentication.getName(), endpointPath)) {
            throw new AccessDeniedException("Quota limit exceeded for endpoint: " + endpointPath);
        }
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }
}
