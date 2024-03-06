package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.ScheduledTasksService;
import fr.dossierfacile.api.front.service.interfaces.StatsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.OperationAccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledTasksServiceImplTest {
    private final TenantCommonRepository tenantRepository = mock(TenantCommonRepository.class);
    private final ConfirmationTokenRepository confirmationTokenRepository = mock(ConfirmationTokenRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final StatsService statsService = mock(StatsService.class);
    private final OperationAccessTokenService operationAccessTokenService = mock(OperationAccessTokenService.class);
    private final ScheduledTasksService scheduledTasksService =
            new ScheduledTasksServiceImpl(tenantRepository, confirmationTokenRepository, operationAccessTokenService, mailService, statsService);

    @BeforeEach
    void beforEach() {
        ReflectionTestUtils.setField(scheduledTasksService, "daysForAccountDeclinationReminder", 5L);
    }

    @Test
    void accountDeclinationReminderWhenAlone() {
        Tenant t = buildTenant(1L, ApplicationType.ALONE);
        when(tenantRepository.findAllDeclinedSinceXDaysAgo(any(), any())).thenReturn(Collections.singletonList(t));
        scheduledTasksService.accountDeclinationReminder();
        verify(mailService).sendEmailWhenAccountIsStillDeclined(any());
    }

    @Test
    void accountDeclinationReminderWhenCouple() {
        Tenant t = buildTenant(2L, ApplicationType.COUPLE);
        Tenant roommate = buildTenant(3L, ApplicationType.COUPLE);
        t.getApartmentSharing().setTenants(List.of(roommate));
        when(tenantRepository.findAllDeclinedSinceXDaysAgo(any(), any())).thenReturn(Collections.singletonList(t));
        scheduledTasksService.accountDeclinationReminder();
        verify(mailService).sendEmailWhenAccountIsStillDeclined(any());
    }

    private static Tenant buildTenant(Long id, ApplicationType applicationType) {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .applicationType(applicationType)
                .build();
        return Tenant.builder()
                .id(id)
                .email("user@dossierfacile.fr")
                .apartmentSharing(apartmentSharing)
                .build();
    }
}