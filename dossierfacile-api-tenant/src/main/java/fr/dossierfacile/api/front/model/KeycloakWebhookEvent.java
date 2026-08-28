package fr.dossierfacile.api.front.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.dossierfacile.common.converter.AcquisitionData;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KeycloakWebhookEvent {
    private String type;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_name")
    private String userName;

    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    private Map<String, List<String>> attributes;

    public AcquisitionData toAcquisitionData() {
        String campaign = getFirstAttribute("acquisition_campaign");
        if (campaign == null) campaign = getFirstAttribute("mtm_campaign");
        if (campaign == null) campaign = getFirstAttribute("utm_campaign");
        if (campaign == null) campaign = getFirstAttribute("campaign");

        String source = getFirstAttribute("acquisition_source");
        if (source == null) source = getFirstAttribute("mtm_source");
        if (source == null) source = getFirstAttribute("utm_source");
        if (source == null) source = getFirstAttribute("source");

        String medium = getFirstAttribute("acquisition_medium");
        if (medium == null) medium = getFirstAttribute("mtm_medium");
        if (medium == null) medium = getFirstAttribute("utm_medium");
        if (medium == null) medium = getFirstAttribute("medium");

        if (campaign == null && source == null && medium == null) {
            return null;
        }

        return AcquisitionData.builder()
                .campaign(campaign)
                .source(source)
                .medium(medium)
                .build();
    }

    public KeycloakUser toKeycloakUser() {
        boolean fc = getFirstAttributeAsBoolean("france-connect");
        String finalEmail = getFirstAttribute("email");
        String finalUsername = getFirstAttribute("username");
        String finalFirstName = getFirstAttribute("firstName");
        String finalLastName = getFirstAttribute("lastName");

        return KeycloakUser.builder()
                .keycloakId(userId)
                .email(finalEmail != null ? finalEmail : email)
                .emailVerified(emailVerified)
                .preferredUsername(finalUsername != null ? finalUsername : userName)
                .givenName(finalFirstName != null ? finalFirstName : firstName)
                .familyName(finalLastName != null ? finalLastName : lastName)
                .gender(getFirstAttribute("gender"))
                .franceConnect(fc)
                .franceConnectSub(getFirstAttribute("france-connect-sub"))
                .franceConnectBirthCountry(getFirstAttribute("birthcountry"))
                .franceConnectBirthPlace(getFirstAttribute("birthplace"))
                .franceConnectBirthDate(getFirstAttribute("birthdate"))
                .build();
    }

    private String getFirstAttribute(String key) {
        if (attributes != null && attributes.containsKey(key)) {
            List<String> values = attributes.get(key);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private boolean getFirstAttributeAsBoolean(String key) {
        String val = getFirstAttribute(key);
        if (val == null || "null".equalsIgnoreCase(val) || val.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(val);
    }
}
