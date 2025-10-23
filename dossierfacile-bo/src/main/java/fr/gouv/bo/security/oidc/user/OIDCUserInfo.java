package fr.gouv.bo.security.oidc.user;

import java.util.Map;

public abstract class OIDCUserInfo {
    protected Map<String, Object> attributes;

    protected OIDCUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    protected Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();

    public abstract String getFirstName();

    public abstract String getLastName();

    public abstract String getEmail();

    public abstract String getImageUrl();
}
