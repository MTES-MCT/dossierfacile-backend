package fr.dossierfacile.api.front.security;

import com.google.common.base.Strings;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final TenantCommonRepository tenantRepository;
    private final TenantService tenantService;
    private final KeycloakService keycloakService;

    private String getUserEmail() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("email");
    }

    private String getFirstName() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("given_name");
    }

    private String getLastName() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("family_name");
    }

    private String getPreferredName() {
        String preferredUsername = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("preferred_username");
        if (Strings.isNullOrEmpty(preferredUsername) || preferredUsername.contains("@")) {
            return null;
        }
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("preferred_username");
    }

    @Override
    public String getKeycloakUserId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("sub");
    }

    private boolean isFranceConnect() {
        var result = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsBoolean("france-connect");
        return Optional.ofNullable(result).orElse(false);
    }

    private String getFranceConnectSub() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("france-connect-sub");
    }

    private String getFranceConnectBirthCountry() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("birthcountry");
    }

    private String getFranceConnectBirthPlace() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("birthplace");
    }

    private String getFranceConnectBirthDate() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("birthdate");
    }

    @Override
    public Tenant getTenant(Long id) {
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_api-partner"))) {
            var keycloakClientId = getKeycloakClientId();
            System.out.println(keycloakClientId);
            if (id == null) {
                //Tenant who logged in to link with the client/partner
                var tenant = getPrincipalAuthTenant();
                if (tenant.getLinkedKeycloakClients() != null && tenant.getLinkedKeycloakClients().contains(keycloakClientId)) {
                    return tenant;
                } else {
                    tenant.addLinkedKeycloakClient(keycloakClientId);
                    return tenantRepository.save(tenant);
                }
            } else {
                //Tenant supposedly already linked with the client/partner
                var tenant = tenantRepository.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
                if (tenant.getLinkedKeycloakClients() != null && tenant.getLinkedKeycloakClients().contains(keycloakClientId)) {
                    return tenant;
                }
                throw new AccessDeniedException("The tenant with ID [" + id + "] isn't linked to the keycloak client [" + keycloakClientId + "]");
            }
        } else {
            return getPrincipalAuthTenant();
        }
    }

    @Override
    public String getKeycloakClientId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
    }

    private Tenant getPrincipalAuthTenant() {
        String email = getUserEmail();
        Optional<Tenant> tenantOptional = tenantRepository.findByEmail(email);
        Tenant tenant;
        if (tenantOptional.isPresent()) {
            tenant = tenantOptional.get();
        } else {
            if (keycloakService.isKeycloakUser(getKeycloakUserId())) {
                log.warn("Tenant " + Obfuscator.email(email) + " not exist - create it");
                tenant = new Tenant(email);
                tenant.setKeycloakId(getKeycloakUserId());
                tenant = tenantService.create(tenant);
            } else {
                throw new AccessDeniedException("invalid token");
            }
        }
        tenant.setKeycloakId(getKeycloakUserId());
        if (!tenant.getFranceConnect().equals(Boolean.TRUE) && isFranceConnect()) {
            tenant.setFranceConnect(isFranceConnect());
            tenant.setFranceConnectSub(getFranceConnectSub());
            tenant.setFranceConnectBirthCountry(getFranceConnectBirthCountry());
            tenant.setFranceConnectBirthPlace(getFranceConnectBirthPlace());
            tenant.setFranceConnectBirthDate(getFranceConnectBirthDate());
            tenant.setFirstName(getFirstName());
            tenant.setLastName(getLastName());
            tenant.setPreferredName(getPreferredName());
        }
        return tenantRepository.saveAndFlush(tenant);
    }
}
