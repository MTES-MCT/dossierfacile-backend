package fr.gouv.bo.security.oidc;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.AuthProvider;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.UserRoleRepository;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.security.oidc.user.KeycloakOIDCUserInfo;
import fr.gouv.bo.security.oidc.user.OIDCUserInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final BOUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        OIDCUserInfo userInfo = new KeycloakOIDCUserInfo(oidcUser.getClaims());

        if (!StringUtils.isNotBlank(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not provided by OIDC provider");
        }

        Optional<BOUser> existingOpt = userRepository.findByEmail(userInfo.getEmail());
        BOUser user;
        if (existingOpt.isPresent()) {
            user = existingOpt.get();
            // Vérifie cohérence provider (si utilisateur legacy google → on migre ou on bloque selon choix; ici on écrase vers keycloak)
            if (!AuthProvider.keycloak.equals(user.getProvider())) {
                user.setProvider(AuthProvider.keycloak);
            }
            user = updateExistingUser(user, userInfo);
        } else {
            user = registerNewUser(userInfo);
        }

        // Rôles existants
        Set<UserRole> roles = userRoleRepository.findAllByUser(user);

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (UserRole ur : roles) {
            authorities.add(new SimpleGrantedAuthority(ur.getRole().name()));
        }

        var principal = UserPrincipal.create(user, oidcUser.getAttributes(), authorities);
        principal.setOidcData(oidcUser.getIdToken(), oidcUser.getUserInfo(), oidcUser.getClaims());

        return principal;
    }

    private BOUser registerNewUser(OIDCUserInfo info) {
        BOUser user = BOUser.builder().build();
        user.setProvider(AuthProvider.keycloak);
        user.setProviderId(info.getId());
        user.setFirstName(info.getFirstName());
        user.setLastName(info.getLastName());
        user.setEmail(info.getEmail());
        user.setImageUrl(info.getImageUrl());
        return userRepository.save(user);
    }

    private BOUser updateExistingUser(BOUser user, OIDCUserInfo info) {
        user.setFirstName(info.getFirstName());
        user.setImageUrl(info.getImageUrl());
        return userRepository.save(user);
    }
}
