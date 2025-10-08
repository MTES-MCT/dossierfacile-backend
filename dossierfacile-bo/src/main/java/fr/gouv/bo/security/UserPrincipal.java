package fr.gouv.bo.security;

import fr.dossierfacile.common.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class UserPrincipal implements UserDetails, OidcUser {
    @Getter
    private final Long id;
    @Getter
    private final String email;
    @Getter
    private final String firstName;

    private final Collection<? extends GrantedAuthority> authorities;
    @Setter
    private transient Map<String, Object> attributes;
    // OIDC fields
    private transient OidcIdToken oidcIdToken;
    private transient OidcUserInfo oidcUserInfo; // nullable
    private transient Map<String, Object> oidcClaims; // nullable

    public UserPrincipal(Long id, String firstName, String email, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.firstName = firstName;
        this.email = email;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user, Set<GrantedAuthority> authorities) {

        return new UserPrincipal(
                user.getId(),
                user.getFirstName(),
                user.getEmail(),
                authorities
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes, Set<GrantedAuthority> authorities) {
        UserPrincipal userPrincipal = UserPrincipal.create(user, authorities);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    public void setOidcData(OidcIdToken idToken, OidcUserInfo userInfo, Map<String, Object> claims) {
        this.oidcIdToken = idToken;
        this.oidcUserInfo = userInfo;
        this.oidcClaims = claims;
        // Pour compat: attributes = claims si non déjà définies
        if (this.attributes == null && claims != null) {
            this.attributes = claims;
        }
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // --- OIDC specific implementations ---
    @Override
    public Map<String, Object> getClaims() {
        return oidcClaims != null ? oidcClaims : attributes; // fallback
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUserInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcIdToken;
    }

    @Override
    public String getName() {
        return firstName;
    }
}
