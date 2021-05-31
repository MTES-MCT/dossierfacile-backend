package fr.dossierfacile.api.front.security.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {
    Authentication getAuthentication();

    User getPrincipalAuth();

    Tenant getPrincipalAuthTenant();
}
