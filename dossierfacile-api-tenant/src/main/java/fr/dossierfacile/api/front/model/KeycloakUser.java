package fr.dossierfacile.api.front.model;

import lombok.Builder;
import lombok.Data;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class KeycloakUser {
    String keycloakId;
    String email;
    boolean emailVerified;
    String preferredUsername;
    String givenName;
    String familyName;
    String gender;
    boolean franceConnect;
    String franceConnectSub;
    String franceConnectBirthCountry;
    String franceConnectBirthPlace;
    String franceConnectBirthDate;

    public String getFcHash() {
        try {
            String valueToHash = String.format("%s%s%s%s%s%s",
                    givenName != null ? givenName : "",
                    familyName != null ? familyName : "",
                    franceConnectBirthDate != null ? franceConnectBirthDate : "",
                    gender != null ? gender : "",
                    franceConnectBirthPlace != null ? franceConnectBirthPlace : "",
                    franceConnectBirthCountry != null ? franceConnectBirthCountry : ""
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valueToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating FranceConnect hash", e);
        }
    }

    public String getComputedPreferredUserName() {
        return Optional.ofNullable(preferredUsername)
                .filter(name -> StringUtils.isNotBlank(name) && !name.contains("@"))
                .orElse(null);
    }
}
