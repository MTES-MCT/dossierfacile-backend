package fr.gouv.bo.security;

import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BOApplicationAccessServiceImpl implements BOApplicationAccessService {

    private static final List<ActionOperatorType> ASSIGNMENT_TYPES =
            List.of(ActionOperatorType.START_PROCESS, ActionOperatorType.STOP_PROCESS);

    private final OperatorLogRepository operatorLogRepository;
    private final TenantCommonRepository tenantRepository;
    private final UserService userService;

    @Override
    public void checkTenantAccess(UserPrincipal principal, Long tenantId) {
        if (isOperatorOnly(principal)) {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            boolean assigned = operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            principal.getId(), tenantId, ASSIGNMENT_TYPES, since);
            if (!assigned) {
                log.warn("OPERATOR id={} attempted to access unassigned tenant id={}", principal.getId(), tenantId);
                throw new AccessDeniedException(
                        "OPERATOR " + principal.getId() + " is not assigned to tenant " + tenantId);
            }
        }
    }

    @Override
    public void checkAndLogApartmentSharingAccess(UserPrincipal principal, Long apartmentSharingId) {
        if (isOperatorOnly(principal)) {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            boolean assigned = operatorLogRepository.existsAssignmentForApartmentSharing(
                    principal.getId(), apartmentSharingId, ASSIGNMENT_TYPES, since);
            if (!assigned) {
                log.warn("OPERATOR id={} attempted to access unassigned apartment sharing id={}", principal.getId(), apartmentSharingId);
                throw new AccessDeniedException(
                        "OPERATOR " + principal.getId() + " is not assigned to apartment sharing " + apartmentSharingId);
            }
        }
        logViewApplicationForApartmentSharing(principal, apartmentSharingId);
    }

    /**
     * Returns true when the authenticated user holds ROLE_OPERATOR but none of
     * ROLE_SUPPORT / ROLE_MANAGER / ROLE_ADMIN.
     * Checks explicit authorities only (not hierarchy-derived ones): CustomOidcUserService
     * adds only the explicitly granted UserRole entries as SimpleGrantedAuthority, so a
     * ROLE_ADMIN user never has ROLE_OPERATOR in their authority set.
     */
    private boolean isOperatorOnly(UserPrincipal principal) {
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains("ROLE_OPERATOR")
                && !authorities.contains("ROLE_SUPPORT")
                && !authorities.contains("ROLE_MANAGER")
                && !authorities.contains("ROLE_ADMIN");
    }

    private void logViewApplicationForApartmentSharing(UserPrincipal principal, Long apartmentSharingId) {
        tenantRepository.findAllByApartmentSharingId(apartmentSharingId).stream()
                .filter(t -> t.getTenantType() == TenantType.CREATE)
                .findFirst()
                .ifPresent(tenant -> {
                    User operator = userService.findUserByEmail(principal.getEmail());
                    operatorLogRepository.save(
                            new OperatorLog(tenant, operator, tenant.getStatus(), ActionOperatorType.VIEW_APPLICATION));
                });
    }
}
