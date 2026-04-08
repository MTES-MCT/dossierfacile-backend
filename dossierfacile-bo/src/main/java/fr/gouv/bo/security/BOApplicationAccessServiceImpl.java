package fr.gouv.bo.security;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.service.QuotaService;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BOApplicationAccessServiceImpl implements BOApplicationAccessService {

    private static final List<ActionOperatorType> ASSIGNMENT_TYPES =
            List.of(ActionOperatorType.START_PROCESS, ActionOperatorType.STOP_PROCESS);

    private final OperatorLogRepository operatorLogRepository;
    private final TenantCommonRepository tenantRepository;
    private final UserService userService;
    private final QuotaService quotaService;

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
        quotaService.checkQuota(principal, ActionOperatorType.VIEW_APPLICATION);
        logViewApplicationForApartmentSharing(principal, apartmentSharingId);
    }

    @Override
    public void checkAndLogSearchTenant(UserPrincipal principal, String query, long resultCount) {
        quotaService.checkQuota(principal, ActionOperatorType.SEARCH_TENANT);
        User operator = userService.findUserByEmail(principal.getEmail());

        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        String normalizedQuery = StringUtils.trimToEmpty(query);
        metadata.put("searchType", detectSearchType(normalizedQuery));
        metadata.put("query", normalizedQuery);
        metadata.put("resultCount", resultCount);

        operatorLogRepository.save(new OperatorLog(
                null,
                operator,
                null,
                ActionOperatorType.SEARCH_TENANT,
                metadata
        ));
    }

    /**
     * Returns true when the authenticated user holds ROLE_OPERATOR but none of
     * ROLE_SUPPORT / ROLE_MANAGER / ROLE_ADMIN.
     * Checks explicit authorities only (not hierarchy-derived ones): CustomOidcUserService
     * adds only the explicitly granted UserRole entries as SimpleGrantedAuthority, so a
     * ROLE_ADMIN user never has ROLE_OPERATOR in their authority set.
     */
    private boolean isOperatorOnly(UserPrincipal principal) {
        return principal.hasAllRoles("ROLE_OPERATOR")
                && principal.hasNoneOfRoles("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN");
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

    private String detectSearchType(String query) {
        if (StringUtils.contains(query, "@")) {
            return "EMAIL";
        }
        if (StringUtils.isNumeric(query)) {
            return "TENANT_ID";
        }
        return "TEXT";
    }
}
