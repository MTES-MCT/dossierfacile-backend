package fr.gouv.bo.domain.policy;

import fr.dossierfacile.common.application.exception.UnauthorizedException;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatorAccessPolicyTest {

    private final OperatorAccessPolicy policy = new OperatorAccessPolicy();

    @Test
    void should_allow_access_when_operator_has_no_operator_role() {
        // Given
        UserRole supportRole = new UserRole(null, Role.ROLE_SUPPORT);
        Operator operator = new Operator(OperatorEntity.builder()
                .id(1L)
                .userRoles(Set.of(supportRole))
                .build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(2L).build());

        // When/Then
        assertThatCode(() -> policy.validateAccess(operator, tenant, false))
                .doesNotThrowAnyException();
    }

    @Test
    void should_allow_access_when_operator_only_has_access() {
        // Given
        UserRole operatorRole = new UserRole(null, Role.ROLE_OPERATOR);
        Operator operator = new Operator(OperatorEntity.builder()
                .id(1L)
                .userRoles(Set.of(operatorRole))
                .build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(2L).build());

        // When/Then
        assertThatCode(() -> policy.validateAccess(operator, tenant, true))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_unauthorized_when_operator_only_and_no_access() {
        // Given
        UserRole operatorRole = new UserRole(null, Role.ROLE_OPERATOR);
        Operator operator = new Operator(OperatorEntity.builder()
                .id(1L)
                .userRoles(Set.of(operatorRole))
                .build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(2L).build());

        // When/Then
        assertThatThrownBy(() -> policy.validateAccess(operator, tenant, false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Operator does not have access to tenant");
    }

    @Test
    void should_allow_access_when_operator_has_mixed_roles_including_manager() {
        // Given
        UserRole operatorRole = new UserRole(null, Role.ROLE_OPERATOR);
        UserRole managerRole = new UserRole(null, Role.ROLE_MANAGER);
        Operator operator = new Operator(OperatorEntity.builder()
                .id(1L)
                .userRoles(Set.of(operatorRole, managerRole))
                .build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(2L).build());

        // When/Then
        assertThatCode(() -> policy.validateAccess(operator, tenant, false))
                .doesNotThrowAnyException();
    }
}
