package fr.gouv.bo.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalRoleTest {

    // -------------------------------------------------------------------------
    // hasAllRoles
    // -------------------------------------------------------------------------

    @Nested
    class HasAllRoles {

        @Test
        void singleRole_present_returnsTrue() {
            assertThat(operator().hasAllRoles("ROLE_OPERATOR")).isTrue();
        }

        @Test
        void singleRole_absent_returnsFalse() {
            assertThat(operator().hasAllRoles("ROLE_SUPPORT")).isFalse();
        }

        @Test
        void multipleRoles_allPresent_returnsTrue() {
            UserPrincipal principal = principal("ROLE_OPERATOR", "ROLE_SUPPORT");
            assertThat(principal.hasAllRoles("ROLE_OPERATOR", "ROLE_SUPPORT")).isTrue();
        }

        @Test
        void multipleRoles_oneAbsent_returnsFalse() {
            assertThat(operator().hasAllRoles("ROLE_OPERATOR", "ROLE_SUPPORT")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // hasAnyRole
    // -------------------------------------------------------------------------

    @Nested
    class HasAnyRole {

        @Test
        void oneOfTheRolesPresent_returnsTrue() {
            assertThat(support().hasAnyRole("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isTrue();
        }

        @Test
        void noneOfTheRolesPresent_returnsFalse() {
            assertThat(operator().hasAnyRole("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isFalse();
        }

        @Test
        void exactMatch_returnsTrue() {
            assertThat(admin().hasAnyRole("ROLE_ADMIN")).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // hasNoneOfRoles
    // -------------------------------------------------------------------------

    @Nested
    class HasNoneOfRoles {

        @Test
        void noRolePresent_returnsTrue() {
            assertThat(operator().hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isTrue();
        }

        @Test
        void oneRolePresent_returnsFalse() {
            assertThat(support().hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isFalse();
        }

        @Test
        void allRolesPresent_returnsFalse() {
            UserPrincipal principal = principal("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN");
            assertThat(principal.hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Composition: isOperatorOnly (as used in BOApplicationAccessServiceImpl)
    // -------------------------------------------------------------------------

    @Nested
    class OperatorOnlyComposition {

        @Test
        void pureOperator_isOperatorOnly() {
            UserPrincipal principal = operator();
            assertThat(principal.hasAllRoles("ROLE_OPERATOR")
                    && principal.hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isTrue();
        }

        @Test
        void support_isNotOperatorOnly() {
            UserPrincipal principal = support();
            assertThat(principal.hasAllRoles("ROLE_OPERATOR")
                    && principal.hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isFalse();
        }

        @Test
        void admin_isNotOperatorOnly() {
            UserPrincipal principal = admin();
            assertThat(principal.hasAllRoles("ROLE_OPERATOR")
                    && principal.hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserPrincipal operator() {
        return principal("ROLE_OPERATOR");
    }

    private UserPrincipal support() {
        return principal("ROLE_SUPPORT");
    }

    private UserPrincipal admin() {
        return principal("ROLE_ADMIN");
    }

    private UserPrincipal principal(String... roles) {
        Set<SimpleGrantedAuthority> authorities = new java.util.HashSet<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return new UserPrincipal(1L, "test", "test@test.com", authorities);
    }
}
