package fr.gouv.bo.security.oidc.user;

import java.util.Map;

public class KeycloakOIDCUserInfo extends OIDCUserInfo {
    public KeycloakOIDCUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getFirstName() {
        return (String) attributes.getOrDefault("given_name", "");
    }

    @Override
    public String getLastName() {
        return (String) attributes.getOrDefault("family_name", "");
    }

    public String getProvider() {
        return (String) attributes.getOrDefault("provider", "keycloak");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        // Keycloak peut exposer une image via claim custom ou avatar URL; absent par défaut.
        Object picture = attributes.get("picture");
        if (picture instanceof String string) {
            return string;
        } else {
            return null;
        }
    }
}

