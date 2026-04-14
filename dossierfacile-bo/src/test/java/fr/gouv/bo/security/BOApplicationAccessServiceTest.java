package fr.gouv.bo.security;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.service.QuotaService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static fr.dossierfacile.common.enums.ActionOperatorType.START_PROCESS;
import static fr.dossierfacile.common.enums.ActionOperatorType.STOP_PROCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BOApplicationAccessServiceTest {

    private static final Long OPERATOR_ID = 10L;
    private static final Long TENANT_ID = 42L;
    private static final Long APARTMENT_SHARING_ID = 100L;

    private OperatorLogRepository operatorLogRepository;
    private TenantCommonRepository tenantRepository;
    private UserService userService;
    private QuotaService quotaService;

    private BOApplicationAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        operatorLogRepository = mock(OperatorLogRepository.class);
        tenantRepository = mock(TenantCommonRepository.class);
        userService = mock(UserService.class);
        quotaService = mock(QuotaService.class);
        service = new BOApplicationAccessServiceImpl(operatorLogRepository, tenantRepository, userService, quotaService);

        // Default: tenant and operator exist
        Tenant tenant = buildTenant(TENANT_ID, TenantType.CREATE, APARTMENT_SHARING_ID);
        when(tenantRepository.findOneById(TENANT_ID)).thenReturn(tenant);
        when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID)).thenReturn(List.of(tenant));
        when(userService.findUserByEmail(any())).thenReturn(new BOUser());
    }

    // -------------------------------------------------------------------------
    // checkTenantAccess
    // -------------------------------------------------------------------------

    @Nested
    class CheckAndLogTenantAccess {

        @Test
        void operator_withStartProcessToday_isAuthorized() {
            UserPrincipal principal = operatorPrincipal();
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                        eq(OPERATOR_ID), eq(TENANT_ID),
                        argThat(types -> types.contains(START_PROCESS)),
                        any(LocalDateTime.class)))
                    .thenReturn(true);

            service.checkTenantAccess(principal, TENANT_ID);
            verifyNoLogWritten();
        }

        @Test
        void operator_withStopProcessToday_isAuthorized() {
            UserPrincipal principal = operatorPrincipal();
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            eq(OPERATOR_ID), eq(TENANT_ID),
                            argThat(types -> types.contains(STOP_PROCESS)),
                            any(LocalDateTime.class)))
                    .thenReturn(true);

            service.checkTenantAccess(principal, TENANT_ID);
            verifyNoLogWritten();
        }

        @Test
        void operator_withNoAssignment_throwsAccessDeniedAndNothingLogged() {
            UserPrincipal principal = operatorPrincipal();
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), any(LocalDateTime.class)))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.checkTenantAccess(principal, TENANT_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verify(operatorLogRepository, never()).save(any());
        }

        @Test
        void operator_withAssignmentYesterday_throwsAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            // existsByOperatorId... returns false regardless of the date passed
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), any(LocalDateTime.class)))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.checkTenantAccess(principal, TENANT_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verify(operatorLogRepository, never()).save(any());
        }

        @Test
        void support_isAlwaysAuthorized() {
            service.checkTenantAccess(supportPrincipal(), TENANT_ID);

            verify(operatorLogRepository, never())
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), any());
            verifyNoLogWritten();
        }

        @Test
        void manager_isAlwaysAuthorized() {
            service.checkTenantAccess(managerPrincipal(), TENANT_ID);

            verify(operatorLogRepository, never())
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), any());
            verifyNoLogWritten();
        }

        @Test
        void admin_isAlwaysAuthorized() {
            service.checkTenantAccess(adminPrincipal(), TENANT_ID);

            verify(operatorLogRepository, never())
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), any());
            verifyNoLogWritten();
        }

        @Test
        @SuppressWarnings("unchecked")
        // Vérifie que le contrôle d'assignation utilise uniquement START_PROCESS et STOP_PROCESS.
        void assignmentCheck_passesStartAndStopProcessTypes() {
            UserPrincipal principal = operatorPrincipal();
            ArgumentCaptor<List<ActionOperatorType>> typesCaptor = ArgumentCaptor.forClass(List.class);
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), typesCaptor.capture(), any()))
                    .thenReturn(true);

            service.checkTenantAccess(principal, TENANT_ID);

            assertThat(typesCaptor.getValue()).containsExactlyInAnyOrder(START_PROCESS, STOP_PROCESS);
        }

        @Test
        // Vérifie que la fenêtre temporelle transmise au repository est bien "maintenant - 24h".
        void assignmentCheck_sinceLast24Hours() {
            UserPrincipal principal = operatorPrincipal();
            ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            LocalDateTime beforeCall = LocalDateTime.now().minusHours(24);
            when(operatorLogRepository
                    .existsByOperatorIdAndTenantIdAndActionOperatorTypeInAndCreationDateGreaterThanEqual(
                            anyLong(), anyLong(), anyList(), sinceCaptor.capture()))
                    .thenReturn(true);

            service.checkTenantAccess(principal, TENANT_ID);
            LocalDateTime afterCall = LocalDateTime.now().minusHours(24);

            LocalDateTime since = sinceCaptor.getValue();
            assertThat(since).isBetween(beforeCall.minusSeconds(1), afterCall.plusSeconds(1));
        }
    }

    // -------------------------------------------------------------------------
    // checkAndLogApartmentSharingAccess
    // -------------------------------------------------------------------------

    @Nested
    class CheckAndLogApartmentSharingAccess {

        @Test
        void operator_withAssignmentForTenantInSharing_isAuthorizedAndLogged() {
            UserPrincipal principal = operatorPrincipal();
            when(operatorLogRepository.existsAssignmentForApartmentSharing(
                    eq(OPERATOR_ID), eq(APARTMENT_SHARING_ID), anyList(), any(LocalDateTime.class)))
                    .thenReturn(true);

            service.checkAndLogApartmentSharingAccess(principal, APARTMENT_SHARING_ID);

            verifyViewApplicationLoggedForApartmentSharing();
        }

        @Test
        void operator_withNoAssignmentInSharing_throwsAccessDeniedAndNothingLogged() {
            UserPrincipal principal = operatorPrincipal();
            when(operatorLogRepository.existsAssignmentForApartmentSharing(
                    anyLong(), anyLong(), anyList(), any(LocalDateTime.class)))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.checkAndLogApartmentSharingAccess(principal, APARTMENT_SHARING_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verify(operatorLogRepository, never()).save(any());
        }

        @Test
        void support_isAlwaysAuthorizedAndLogged() {
            service.checkAndLogApartmentSharingAccess(supportPrincipal(), APARTMENT_SHARING_ID);

            verify(operatorLogRepository, never())
                    .existsAssignmentForApartmentSharing(anyLong(), anyLong(), anyList(), any());
            verifyViewApplicationLoggedForApartmentSharing();
        }

        @Test
        void manager_isAlwaysAuthorizedAndLogged() {
            service.checkAndLogApartmentSharingAccess(managerPrincipal(), APARTMENT_SHARING_ID);

            verifyViewApplicationLoggedForApartmentSharing();
        }

        @Test
        void admin_isAlwaysAuthorizedAndLogged() {
            service.checkAndLogApartmentSharingAccess(adminPrincipal(), APARTMENT_SHARING_ID);

            verifyViewApplicationLoggedForApartmentSharing();
        }

        @Test
        // Vérifie que le log VIEW_APPLICATION sur la vue colocation est rattaché au tenant CREATE.
        void log_isAnchoredToCreateTenant() {
            UserPrincipal principal = supportPrincipal();
            Tenant createTenant = buildTenant(TENANT_ID, TenantType.CREATE, APARTMENT_SHARING_ID);
            Tenant joinTenant = buildTenant(99L, TenantType.JOIN, APARTMENT_SHARING_ID);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(joinTenant, createTenant));

            service.checkAndLogApartmentSharingAccess(principal, APARTMENT_SHARING_ID);

            ArgumentCaptor<OperatorLog> logCaptor = ArgumentCaptor.forClass(OperatorLog.class);
            verify(operatorLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getTenant()).isSameAs(createTenant);
            assertThat(logCaptor.getValue().getActionOperatorType()).isEqualTo(ActionOperatorType.VIEW_APPLICATION);
        }
    }

    @Nested
    class CheckAndLogSearchTenant {

        @Test
        void checkAndLogSearchTenant_logsExpectedMetadata() {
            UserPrincipal principal = supportPrincipal();

            service.checkAndLogSearchTenant(principal, "john.doe@example.com", 2L);

            ArgumentCaptor<OperatorLog> captor = ArgumentCaptor.forClass(OperatorLog.class);
            verify(operatorLogRepository).save(captor.capture());
            OperatorLog log = captor.getValue();

            assertThat(log.getActionOperatorType()).isEqualTo(ActionOperatorType.SEARCH_TENANT);
            assertThat(log.getTenant()).isNull();
            assertThat(log.getTenantFileStatus()).isNull();
            assertThat(log.getMetadata().get("searchType").asText()).isEqualTo("EMAIL");
            assertThat(log.getMetadata().get("query").asText()).isEqualTo("john.doe@example.com");
            assertThat(log.getMetadata().get("resultCount").asLong()).isEqualTo(2L);
        }

        @Test
        void checkAndLogSearchTenant_withoutAnchorTenant_logsWithNullTenant() {
            UserPrincipal principal = supportPrincipal();

            service.checkAndLogSearchTenant(principal, "unknown@example.com", 0L);

            ArgumentCaptor<OperatorLog> captor = ArgumentCaptor.forClass(OperatorLog.class);
            verify(operatorLogRepository).save(captor.capture());
            OperatorLog log = captor.getValue();

            assertThat(log.getActionOperatorType()).isEqualTo(ActionOperatorType.SEARCH_TENANT);
            assertThat(log.getTenant()).isNull();
            assertThat(log.getTenantFileStatus()).isNull();
            assertThat(log.getMetadata().get("searchType").asText()).isEqualTo("EMAIL");
            assertThat(log.getMetadata().get("query").asText()).isEqualTo("unknown@example.com");
            assertThat(log.getMetadata().get("resultCount").asLong()).isZero();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void verifyNoLogWritten() {
        verify(operatorLogRepository, never()).save(any());
    }

    private void verifyViewApplicationLoggedForApartmentSharing() {
        ArgumentCaptor<OperatorLog> captor = ArgumentCaptor.forClass(OperatorLog.class);
        verify(operatorLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActionOperatorType()).isEqualTo(ActionOperatorType.VIEW_APPLICATION);
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(OPERATOR_ID, "operator", "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(20L, "support", "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
    }

    private UserPrincipal managerPrincipal() {
        return new UserPrincipal(21L, "manager", "manager@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    private UserPrincipal adminPrincipal() {
        return new UserPrincipal(22L, "admin", "admin@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Tenant buildTenant(Long id, TenantType type, Long apartmentSharingId) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setTenantType(type);
        tenant.setStatus(TenantFileStatus.TO_PROCESS);
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(apartmentSharingId);
        tenant.setApartmentSharing(apartmentSharing);
        return tenant;
    }
}
