package fr.gouv.bo.domain.policy;

import fr.dossierfacile.common.application.exception.UnauthorizedException;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

// Je ne sais pas trop si on ne s'autoriserait pas à mettre ça dans le BOApplicationAccessServiceImpl (qui existe déjà)
@Slf4j
@Component
public class OperatorAccessPolicy {

    public void validateAccess(Operator operator, Tenant tenant, boolean hasAccessToTenant) {
        if (isOperatorOnly(operator)) {
            if (!hasAccessToTenant) {
                log.warn("OPERATOR id={} attempted to access unassigned tenant id={}", operator.getId(), tenant.getId());
                throw new UnauthorizedException("Operator does not have access to tenant");
            }
        }
    }

    private boolean isOperatorOnly(Operator operator) {
        var roles = operator.getUserRoles().stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        return roles.contains(fr.dossierfacile.common.enums.Role.ROLE_OPERATOR)
                && !roles.contains(fr.dossierfacile.common.enums.Role.ROLE_SUPPORT)
                && !roles.contains(fr.dossierfacile.common.enums.Role.ROLE_MANAGER)
                && !roles.contains(fr.dossierfacile.common.enums.Role.ROLE_ADMIN);
    }
}
