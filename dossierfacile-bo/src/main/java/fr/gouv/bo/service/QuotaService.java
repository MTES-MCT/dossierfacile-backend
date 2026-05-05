package fr.gouv.bo.service;

import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
public class QuotaService {

    @Value("${quota.bo.start-process.daily:350}")
    private int limitStartProcess;

    @Value("${quota.bo.view-application.daily:200}")
    private int limitViewApplication;

    @Value("${quota.bo.search-tenant.daily:100}")
    private int limitSearchTenant;

    private final OperatorLogRepository operatorLogRepository;

    public QuotaService(OperatorLogRepository operatorLogRepository) {
        this.operatorLogRepository = operatorLogRepository;
    }

    /**
     * Checks the daily quota for the given action and principal.
     * Throws AccessDeniedException if the day's limit is reached.
     * SEARCH_TENANT is silently skipped for roles that cannot search (OPERATOR).
     */
    public void checkQuota(UserPrincipal principal, ActionOperatorType action) {
        if (action == ActionOperatorType.SEARCH_TENANT && !hasSupportOrAbove(principal)) {
            return;
        }

        int limit = limitFor(action);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = operatorLogRepository
                .countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(
                        principal.getId(), action, startOfDay);

        if (count >= limit) {
            log.warn("Quota exceeded: userId={} action={} count={} limit={}",
                    principal.getId(), action, count, limit);
            throw new AccessDeniedException(
                    "Quota journalier atteint pour l'action " + action + " (limite : " + limit + ")");
        }
    }

    private int limitFor(ActionOperatorType action) {
        return switch (action) {
            case START_PROCESS    -> limitStartProcess;
            case VIEW_APPLICATION -> limitViewApplication;
            case SEARCH_TENANT    -> limitSearchTenant;
            case STOP_PROCESS     -> throw new IllegalArgumentException(
                    "No quota is defined for action " + action);
        };
    }

    private boolean hasSupportOrAbove(UserPrincipal principal) {
        return principal.hasAnyRole("ROLE_SUPPORT", "ROLE_MANAGER", "ROLE_ADMIN");
    }
}
