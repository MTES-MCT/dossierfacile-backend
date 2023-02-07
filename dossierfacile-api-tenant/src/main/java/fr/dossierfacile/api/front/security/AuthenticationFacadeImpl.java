package fr.dossierfacile.api.front.security;

import com.google.common.base.Strings;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.exception.TenantUserApiNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.api.front.util.Obfuscator;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final TenantCommonRepository tenantRepository;
    private final TenantUserApiRepository tenantUserApiRepository;
    private final TenantStatusService tenantStatusService;
    private final TenantService tenantService;
    private final KeycloakService keycloakService;
    private final LogService logService;
    private final DocumentService documentService;
    @Value("${keycloak.server.url}")
    private String keycloakServerUrl;
    @Value("${keycloak.franceconnect.provider}")
    private String provider;
    @Value("${keycloak.server.realm}")
    private String realm;

    @Override
    public String getUserEmail() {
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
        if (id != null) {
            if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_api-partner"))
            ) {
                var keycloakClientId = getKeycloakClientId();
                //Tenant supposedly already linked with the client/partner
                return tenantUserApiRepository.findFirstByTenantIdAndUserApiName(id, keycloakClientId)
                        .orElseThrow(() -> new TenantUserApiNotFoundException(id, keycloakClientId))
                        .getTenant();
            }
            if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_dossier"))) {
                return tenantRepository.findById(id)
                        .orElseThrow(() -> new TenantNotFoundException(id));

            } else {
                throw new AccessDeniedException("Access denied id=" + id);
            }
        } else {
            return getLoggedTenant();
        }
    }

    @Override
    public String getKeycloakClientId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
    }

    @Override
    public Tenant getLoggedTenant() {
        String email = getUserEmail();
        if (!Boolean.TRUE.equals(((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsBoolean("email_verified"))) {
            throw new AccessDeniedException("Email has not been verified");
        }
        Optional<Tenant> tenantOptional = tenantRepository.findByEmail(email);
        Tenant tenant;
        if (tenantOptional.isPresent()) {
            tenant = tenantOptional.get();
            if (!Boolean.TRUE.equals(tenant.getFranceConnect()) && isFranceConnect()) {
                log.info("Local account link to FranceConnect account, for tenant with ID {}", tenant.getId());
                logService.saveLog(LogType.FC_ACCOUNT_LINK, tenant.getId());
            }
        } else {
            if (keycloakService.isKeycloakUser(getKeycloakUserId())) {
                log.warn("Tenant " + Obfuscator.email(email) + " not exist - create it");
                tenant = new Tenant(email);
                tenant.setKeycloakId(getKeycloakUserId());
                tenant = tenantService.create(tenant);
                if (isFranceConnect()) {
                    log.info("Local account creation via FranceConnect account, for tenant with ID {}", tenant.getId());
                    logService.saveLog(LogType.FC_ACCOUNT_CREATION, tenant.getId());
                }
            } else {
                throw new AccessDeniedException("invalid token");
            }
        }
        tenant.setKeycloakId(getKeycloakUserId());
        if (!Boolean.TRUE.equals(tenant.getFranceConnect()) && isFranceConnect()) {
            tenant.setFranceConnect(isFranceConnect());
            tenant.setFranceConnectSub(getFranceConnectSub());
            tenant.setFranceConnectBirthCountry(getFranceConnectBirthCountry());
            tenant.setFranceConnectBirthPlace(getFranceConnectBirthPlace());
            tenant.setFranceConnectBirthDate(getFranceConnectBirthDate());

            if (!StringUtils.equals(tenant.getFirstName(), getFirstName())
                    || !StringUtils.equals(tenant.getLastName(), getLastName())
                    || (getPreferredName() != null && !StringUtils.equals(tenant.getPreferredName(), getPreferredName()))) {
                if (tenant.getStatus() == TenantFileStatus.VALIDATED) {
                    documentService.resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(tenant.getDocuments(),
                            Arrays.asList(DocumentCategory.values()));
                }
            }
            tenant.setFirstName(getFirstName());
            tenant.setLastName(getLastName());
            tenant.setPreferredName(getPreferredName() == null ? tenant.getPreferredName() : getPreferredName());
            tenantStatusService.updateTenantStatus(tenant);
        }
        return tenantRepository.saveAndFlush(tenant);
    }

    @Override
    public String getFranceConnectLink(String redirectUri) {
        String clientId = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
        String token = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("session_state");
        String nonce = UUID.randomUUID().toString();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String input = nonce + token + clientId + "oidc";
        byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
        String hash = Base64Url.encode(check);

        return KeycloakUriBuilder.fromUri(keycloakServerUrl)
                .path("/realms/{realm}/broker/{provider}/link")
                .queryParam("nonce", nonce)
                .queryParam("hash", hash)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri).build(realm, provider).toString();
    }

    @Override
    public KeycloakUser getKeycloakUser(){
        var jwt = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return KeycloakUser.builder()
                .keycloakId(jwt.getClaimAsString("sub"))
                .email(jwt.getClaimAsString("email"))
                .givenName(jwt.getClaimAsString("given_name"))
                .familyName(jwt.getClaimAsString("family_name"))
                .preferredUsername(getPreferredName())
                .franceConnect(Boolean.TRUE.equals(jwt.getClaimAsBoolean("france-connect")))
                .build();
    }
}
