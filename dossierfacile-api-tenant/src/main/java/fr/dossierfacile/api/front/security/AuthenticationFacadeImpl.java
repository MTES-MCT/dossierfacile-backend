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
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
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
    private final TenantPermissionsService tenantPermissionsService;
    private final TenantStatusService tenantStatusService;
    private final TenantService tenantService;
    private final LogService logService;
    private final DocumentService documentService;
    @Value("${keycloak.server.url}")
    private String keycloakServerUrl;
    @Value("${keycloak.franceconnect.provider}")
    private String provider;
    @Value("${keycloak.server.realm}")
    private String realm;

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
                .preferredUsername(
                        Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                                .filter(name -> StringUtils.isNotBlank(name) && !name.contains("@"))
                                .orElse(null))
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
        Tenant tenant = findOrCreateTenant(kcUser, acquisitionData);
        return synchronizeTenant(tenant, kcUser);
    }

    private Tenant findOrCreateTenant(KeycloakUser kcUser, AcquisitionData acquisitionData) {
        String keycloakId = kcUser.getKeycloakId();
        Tenant tenant = tenantRepository.findByKeycloakId(keycloakId);
        if (tenant != null) {
            return tenant;
        }
        log.warn("No tenant account found associated with keycloakId {}", keycloakId);
        Optional<Tenant> tenantByEmail = tenantRepository.findByEmail(kcUser.getEmail());
        if (tenantByEmail.isPresent()) {
            log.info("Found tenant by email from keycloak");
            return tenantByEmail.get();
        }
        log.info("Creating tenant account associated with keycloakId {}", keycloakId);
        return tenantService.registerFromKeycloakUser(kcUser, null, acquisitionData);
    }

    private Tenant synchronizeTenant(Tenant tenant, KeycloakUser user) {
        // check if some data should be updated
        if (!matches(tenant, user)) {
            if (!StringUtils.equalsIgnoreCase(tenant.getEmail(), user.getEmail())) {
                //Don't automatically merge
                log.error("Tenant email and current logged email mismatch FC? '%s' vs '%s' - tenant won't be synchronized".formatted(tenant.getEmail(), user.getEmail()));
                tenant.setWarningMessage("Attention, l'email de compte est '%s' et l'email de connexion est '%s'".formatted(tenant.getEmail(), user.getEmail()));
                return tenant;
            }

            // update tenant from keycloakUser
            if (!Boolean.TRUE.equals(tenant.getFranceConnect()) && user.isFranceConnect()) {
                log.info("Local account link to FranceConnect account, for tenant with ID {}", tenant.getId());
                logService.saveLog(LogType.FC_ACCOUNT_LINK, tenant.getId());
            } else if (tenant.getKeycloakId() == null ){
                log.info("First tenant connection from DF, for tenant with ID {}", tenant.getId());
                logService.saveLog(LogType.ACCOUNT_LINK, tenant.getId());
            }
            tenant.setKeycloakId(user.getKeycloakId());

            tenant.setFranceConnect(user.isFranceConnect());
            tenant.setFranceConnectSub(user.getFranceConnectSub());
            tenant.setFranceConnectBirthCountry(user.getFranceConnectBirthCountry());
            tenant.setFranceConnectBirthPlace(user.getFranceConnectBirthPlace());
            tenant.setFranceConnectBirthDate(user.getFranceConnectBirthDate());

            // If the owner type is SELF, we do not update the name of the tenant otherwise we will overwrite the name of the account
            if (user.isFranceConnect() && tenant.getOwnerType() == TenantOwnerType.SELF) {
                if (!StringUtils.equals(tenant.getFirstName(), user.getGivenName())
                        || !StringUtils.equals(tenant.getLastName(), user.getFamilyName())
                        || (user.getPreferredUsername() != null && !StringUtils.equals(tenant.getPreferredName(), user.getPreferredUsername()))) {
                    documentService.resetValidatedOrInProgressDocumentsAccordingCategories(tenant.getDocuments(),
                            Arrays.asList(DocumentCategory.values()));
                }
                tenant.setFirstName(user.getGivenName());
                tenant.setLastName(user.getFamilyName());
                tenant.setPreferredName(user.getPreferredUsername() == null ? tenant.getPreferredName() : user.getPreferredUsername());
            }
            tenantStatusService.updateTenantStatus(tenant);

            return tenantRepository.saveAndFlush(tenant);
        }
        return tenant;
    }

    // Todo : This method should maybe return false if user is france connected and names are different (for the moment the first name and the lastname need to be different)
    private boolean matches(Tenant tenant, KeycloakUser user) {
        return StringUtils.equals(tenant.getKeycloakId(), user.getKeycloakId())
                && StringUtils.equals(tenant.getEmail(), user.getEmail())
                && tenant.getFranceConnect() == user.isFranceConnect()
                && (!user.isFranceConnect() ||
                // TODO : The || should be a &&
                (StringUtils.equalsIgnoreCase(tenant.getFirstName(), user.getGivenName()) ||
                        StringUtils.equalsIgnoreCase(tenant.getLastName(), user.getFamilyName())
                ));
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
}
