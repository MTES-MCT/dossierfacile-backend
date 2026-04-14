package fr.gouv.bo.service;

import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuotaServiceTest {

    private static final Long USER_ID = 1L;

    private OperatorLogRepository operatorLogRepository;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        operatorLogRepository = mock(OperatorLogRepository.class);
        quotaService = new QuotaService(operatorLogRepository);
        // Inject the same default values used in the QuotaService (@Value defaults)
        ReflectionTestUtils.setField(quotaService, "limitStartProcess", 2000);
        ReflectionTestUtils.setField(quotaService, "limitViewApplication", 500);
        ReflectionTestUtils.setField(quotaService, "limitSearchTenant", 100);
        // Default: no actions logged today
        when(operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(
                any(), any(), any())).thenReturn(0L);
    }

    // -------------------------------------------------------------------------
    // START_PROCESS — limit 2000
    // -------------------------------------------------------------------------

    @Nested
    class StartProcess {

        @Test
        void underLimit_doesNotThrow() {
            stubCount(ActionOperatorType.START_PROCESS, 1999L);
            var principal = operatorPrincipal();

            assertThatCode(() -> quotaService.checkQuota(principal, ActionOperatorType.START_PROCESS))
                    .doesNotThrowAnyException();
        }

        @Test
        void atLimit_throwsAccessDeniedException() {
            stubCount(ActionOperatorType.START_PROCESS, 2000L);
            var principal = operatorPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.START_PROCESS))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void atLimit_alsoAppliesForSupportRole() {
            stubCount(ActionOperatorType.START_PROCESS, 2000L);
            var principal = supportPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.START_PROCESS))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // -------------------------------------------------------------------------
    // VIEW_APPLICATION — limit 500
    // -------------------------------------------------------------------------

    @Nested
    class ViewApplication {

        @Test
        void underLimit_doesNotThrow() {
            stubCount(ActionOperatorType.VIEW_APPLICATION, 499L);
            var principal = operatorPrincipal();

            assertThatCode(() -> quotaService.checkQuota(principal, ActionOperatorType.VIEW_APPLICATION))
                    .doesNotThrowAnyException();
        }

        @Test
        void atLimit_throwsAccessDeniedException() {
            stubCount(ActionOperatorType.VIEW_APPLICATION, 500L);
            var principal = operatorPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.VIEW_APPLICATION))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void atLimit_alsoAppliesForAdminRole() {
            stubCount(ActionOperatorType.VIEW_APPLICATION, 500L);
            var principal = adminPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.VIEW_APPLICATION))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // -------------------------------------------------------------------------
    // SEARCH_TENANT — limit 100, only for SUPPORT/MANAGER/ADMIN
    // -------------------------------------------------------------------------

    @Nested
    class SearchTenant {

        @Test
        void support_underLimit_doesNotThrow() {
            stubCount(ActionOperatorType.SEARCH_TENANT, 99L);
            var principal = supportPrincipal();

            assertThatCode(() -> quotaService.checkQuota(principal, ActionOperatorType.SEARCH_TENANT))
                    .doesNotThrowAnyException();
        }

        @Test
        void support_atLimit_throwsAccessDeniedException() {
            stubCount(ActionOperatorType.SEARCH_TENANT, 100L);
            var principal = supportPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.SEARCH_TENANT))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void admin_atLimit_throwsAccessDeniedException() {
            stubCount(ActionOperatorType.SEARCH_TENANT, 100L);
            var principal = adminPrincipal();

            assertThatThrownBy(() -> quotaService.checkQuota(principal, ActionOperatorType.SEARCH_TENANT))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void stubCount(ActionOperatorType action, long count) {
        when(operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(
                eq(USER_ID), eq(action), any(LocalDateTime.class))).thenReturn(count);
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(USER_ID, "operator", "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(USER_ID, "support", "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
    }

    private UserPrincipal adminPrincipal() {
        return new UserPrincipal(USER_ID, "admin", "admin@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
