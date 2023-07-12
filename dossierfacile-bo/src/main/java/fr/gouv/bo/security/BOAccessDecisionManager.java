package fr.gouv.bo.security;

import fr.gouv.bo.service.QuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Component
@Slf4j
public class BOAccessDecisionManager implements AccessDecisionManager {
    private final QuotaService quotaService;
    private final AccessDecisionVoter<FilterInvocation> expressionVoter;

    public BOAccessDecisionManager(QuotaService quotaService) {
        this.quotaService = quotaService;
        this.expressionVoter = new WebExpressionVoter();
    }

    private boolean checkExpression(Authentication authentication, FilterInvocation object, Collection<ConfigAttribute> attributes) {
        List<ConfigAttribute> singleAttributeList = new ArrayList(1);
        singleAttributeList.add(null);

        for (ConfigAttribute attribute : attributes) {
            if (!expressionVoter.supports(attribute))
                continue;
            singleAttributeList.set(0, attribute);
            if (expressionVoter.vote(authentication, object, singleAttributeList) == -1) {
                return false;
            }
        }
        return !CollectionUtils.isEmpty(attributes);
    }

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {

        if (!checkExpression(authentication, (FilterInvocation) object, configAttributes)) {
            throw new AccessDeniedException("Access is denied");
        }

        String endpointPath = ((FilterInvocation) object).getRequestUrl();
        if (!quotaService.checkQuota(authentication.getName(), endpointPath)) {
            throw new AccessDeniedException("Vous avez atteint la limite !");
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
