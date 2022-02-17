package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final TenantCommonRepository tenantRepository;
    private final ApartmentSharingService apartmentSharingService;
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
        Optional<Tenant> tenantOptional = tenantRepository.findByEmail(getUserEmail());
        Tenant tenant;
        if (tenantOptional.isPresent()) {
            tenant = tenantOptional.get();
        } else {
            if (keycloakService.isKeycloakUser(getKeycloakUserId())) {
                tenant = new Tenant(getUserEmail());
                tenant.setKeycloakId(getKeycloakUserId());
                tenantRepository.save(tenant);
                apartmentSharingService.createApartmentSharing(tenant);
            }
            throw new AccessDeniedException("invalid token");
        }
        tenant.setKeycloakId(getKeycloakUserId());
        if (isFranceConnect()) {
            tenant.setFranceConnect(isFranceConnect());
            tenant.setFranceConnectSub(getFranceConnectSub());
            tenant.setFranceConnectBirthCountry(getFranceConnectBirthCountry());
            tenant.setFranceConnectBirthPlace(getFranceConnectBirthPlace());
            tenant.setFranceConnectBirthDate(getFranceConnectBirthDate());
            tenant.setFirstName(getFirstName());
            tenant.setLastName(getLastName());
        }
        return tenantRepository.saveAndFlush(tenant);
    }
}
