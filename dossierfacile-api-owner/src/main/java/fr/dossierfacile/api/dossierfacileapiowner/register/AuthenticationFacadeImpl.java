package fr.dossierfacile.api.dossierfacileapiowner.register;

import com.google.common.base.Strings;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerRepository;
import fr.dossierfacile.common.entity.Owner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    private final OwnerRepository ownerRepository;
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
    public Owner getOwner() {
        if (!keycloakService.isKeycloakUser(getKeycloakUserId())) {
            throw new AccessDeniedException("invalid token");
        }
        Optional<Owner> optionalOwner = ownerRepository.findByKeycloakId(getKeycloakUserId());
        if (optionalOwner.isEmpty()) {
            optionalOwner = ownerRepository.findByEmail(getUserEmail());
        }
        Owner owner = optionalOwner.orElse(Owner.builder().email(getUserEmail()).build());
        owner.setKeycloakId(getKeycloakUserId());
        owner.setFranceConnect(isFranceConnect());
        if (isFranceConnect()) {
            owner.setFranceConnectSub(getFranceConnectSub());
            owner.setFranceConnectBirthCountry(getFranceConnectBirthCountry());
            owner.setFranceConnectBirthPlace(getFranceConnectBirthPlace());
            owner.setFranceConnectBirthDate(getFranceConnectBirthDate());
            owner.setFirstName(getFirstName());
            owner.setLastName(getLastName());
            owner.setPreferredName(getPreferredName());
        }
        return ownerRepository.saveAndFlush(owner);
    }

    @Override
    public String getKeycloakClientId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
    }

}
