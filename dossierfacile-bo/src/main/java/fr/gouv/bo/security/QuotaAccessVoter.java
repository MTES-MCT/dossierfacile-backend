package fr.gouv.bo.security;

import fr.gouv.bo.service.QuotaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;

import java.util.Collection;


@Component
@AllArgsConstructor
@Slf4j
public class QuotaAccessVoter implements AccessDecisionVoter<Object> {
    private QuotaService quotaService;

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        String endpointPath = ((FilterInvocation) object).getRequestUrl();

        if (!quotaService.checkQuota(authentication.getName(), endpointPath)) {
            return ACCESS_DENIED;
        }
        return ACCESS_GRANTED;
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
