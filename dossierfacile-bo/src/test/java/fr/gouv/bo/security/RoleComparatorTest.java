package fr.gouv.bo.security;

import fr.dossierfacile.common.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleComparatorTest {

    private RoleHierarchy buildHierarchy() {
        RoleHierarchyImpl impl = new RoleHierarchyImpl();
        impl.setHierarchy(String.join("\n",
                "ROLE_ADMIN > ROLE_MANAGER",
                "ROLE_MANAGER > ROLE_OPERATOR",
                "ROLE_OPERATOR > ROLE_SUPPORT"
        ));
        return impl;
    }

    @Test
    void highest_is_admin_when_admin_present() {
        RoleService comparator = new RoleService(buildHierarchy());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        Role highest = comparator.getHighestRole(authorities);
        assertThat(highest).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void highest_is_manager_when_only_manager() {
        RoleService comparator = new RoleService(buildHierarchy());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
        Role highest = comparator.getHighestRole(authorities);
        assertThat(highest).isEqualTo(Role.ROLE_MANAGER);
    }

    @Test
    void highest_support_when_only_support() {
        RoleService comparator = new RoleService(buildHierarchy());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPPORT"));
        Role highest = comparator.getHighestRole(authorities);
        assertThat(highest).isEqualTo(Role.ROLE_SUPPORT);
    }

    @Test
    void returns_null_when_only_non_enum_role() {
        RoleService comparator = new RoleService(buildHierarchy());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER")); // Non présent dans l'enum Role
        Role highest = comparator.getHighestRole(authorities);
        assertThat(highest).isNull();
    }

    @Test
    void returns_null_when_empty_or_null() {
        RoleService comparator = new RoleService(buildHierarchy());
        assertThat(comparator.getHighestRole(List.of())).isNull();
        assertThat(comparator.getHighestRole(null)).isNull();
    }

    @Test
    void ge_true_when_userRole_implies_target() {
        RoleService comparator = new RoleService(buildHierarchy());
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_ADMIN, Role.ROLE_MANAGER)).isTrue();
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_MANAGER, Role.ROLE_OPERATOR)).isTrue();
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_OPERATOR, Role.ROLE_SUPPORT)).isTrue();
    }

    @Test
    void ge_true_when_equal_roles() {
        RoleService comparator = new RoleService(buildHierarchy());
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_MANAGER, Role.ROLE_MANAGER)).isTrue();
    }

    @Test
    void ge_false_when_lower_than_target() {
        RoleService comparator = new RoleService(buildHierarchy());
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_OPERATOR, Role.ROLE_MANAGER)).isFalse();
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_SUPPORT, Role.ROLE_OPERATOR)).isFalse();
    }

    @Test
    void ge_false_when_null_inputs() {
        RoleService comparator = new RoleService(buildHierarchy());
        assertThat(comparator.isRoleGreaterOrEqual(null, Role.ROLE_MANAGER)).isFalse();
        assertThat(comparator.isRoleGreaterOrEqual(Role.ROLE_MANAGER, null)).isFalse();
    }
}
