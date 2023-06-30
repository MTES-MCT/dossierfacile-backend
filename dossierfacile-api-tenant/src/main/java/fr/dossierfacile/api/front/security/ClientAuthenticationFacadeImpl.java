package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.ClientNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientAuthenticationFacadeImpl implements ClientAuthenticationFacade {

    private final UserApiService userApiService;

    @Override
    public String getKeycloakClientId() {
        try {
            return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("clientId");
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public UserApi getClient() {
        return userApiService.findByName(getKeycloakClientId()).orElseThrow(ClientNotFoundException::new);
    }
}