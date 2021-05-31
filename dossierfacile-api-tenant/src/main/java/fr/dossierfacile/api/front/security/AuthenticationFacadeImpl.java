package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.exception.UserNotFoundException;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public User getPrincipalAuth() {
        return userRepository.findByEmail(getAuthentication().getName())
                .orElseThrow(() -> new UserNotFoundException(getAuthentication().getName()));
    }

    @Override
    public Tenant getPrincipalAuthTenant() {
        return tenantRepository.findByEmail(getAuthentication().getName())
                .orElseThrow(() -> new TenantNotFoundException(getAuthentication().getName()));
    }
}
