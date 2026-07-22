package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import fr.dossierfacile.api.front.domain.service.FindOrCreateTenantDomainService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final TenantCommonRepository tenantRepository;
    private final TenantPermissionsService tenantPermissionsService;
    private final FindOrCreateTenantDomainService findOrCreateTenantDomainService;

    @Override
    public String getKeycloakClientId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
    }

    @Override
    public String getUserEmail() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("email");
    }

    @Override
    public String getKeycloakUserId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("sub");
    }

    @Override
    public KeycloakUser getKeycloakUser() {
        var jwt = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        return KeycloakUser.builder()
                .keycloakId(jwt.getClaimAsString("sub"))
                .email(jwt.getClaimAsString("email"))
                .givenName(jwt.getClaimAsString("given_name"))
                .familyName(jwt.getClaimAsString("family_name"))
                .gender(jwt.getClaimAsString("gender"))
                .preferredUsername(jwt.getClaimAsString("preferred_username"))
                .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                .franceConnect(Boolean.TRUE.equals(jwt.getClaimAsBoolean("france-connect")))
                .franceConnectSub(jwt.getClaimAsString("france-connect-sub"))
                .franceConnectBirthCountry(jwt.getClaimAsString("birthcountry"))
                .franceConnectBirthPlace(jwt.getClaimAsString("birthplace"))
                .franceConnectBirthDate(jwt.getClaimAsString("birthdate"))
                .build();
    }

    @Override
    public Tenant getTenant(Long tenantId) {
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_dossier"))) {
            throw new AccessDeniedException("Access denied id=" + tenantId);
        }
        if (tenantId != null) {
            if (!tenantPermissionsService.canAccess(getKeycloakUserId(), tenantId)) {
                throw new AccessDeniedException("Access denied id=" + tenantId);
            }
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new TenantNotFoundException(tenantId));
        }
        return getLoggedTenant();
    }

    @Override
    public Tenant getLoggedTenant() {
        return getLoggedTenant(null);
    }

    @Override
    public Tenant getLoggedTenant(AcquisitionData acquisitionData) {
        KeycloakUser kcUser = getKeycloakUser();
        if (!kcUser.isEmailVerified() && !kcUser.isFranceConnect()) {
            throw new AccessDeniedException("Email is not verified" + kcUser.getEmail());
        }
        return findOrCreateTenantDomainService.findOrCreateTenant(kcUser, acquisitionData);
    }
}
