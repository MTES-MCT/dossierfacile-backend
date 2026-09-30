package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.logging.task.LogAggregator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationReminderTaskTest {

    @Mock
    private CompletedOptinTenantRepository tenantRepository;
    @Mock
    private ValidationReminderService validationReminderService;

    private ValidationReminderTask task;

    @BeforeEach
    void setUp() {
        // Initialize the task manually to override the @Lookup method
        task = new ValidationReminderTask(tenantRepository, validationReminderService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };
        ReflectionTestUtils.setField(task, "minAgeHours", 24L);
        ReflectionTestUtils.setField(task, "maxAgeHours", 72L);
    }

    @Test
    void should_select_the_tenants_submitted_between_72_and_24_hours_ago() {
        when(tenantRepository.findTenantIdsToRemindForValidation(any(), any())).thenReturn(List.of());

        task.sendValidationReminders();

        ArgumentCaptor<LocalDateTime> submittedFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> submittedBefore = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tenantRepository).findTenantIdsToRemindForValidation(submittedFrom.capture(), submittedBefore.capture());
        assertThat(Duration.between(submittedFrom.getValue(), submittedBefore.getValue())).isEqualTo(Duration.ofHours(48));
        assertThat(Duration.between(submittedBefore.getValue(), LocalDateTime.now()))
                .isBetween(Duration.ofHours(24), Duration.ofHours(24).plusMinutes(1));
    }

    @Test
    void should_keep_reminding_the_other_tenants_when_one_fails() {
        when(tenantRepository.findTenantIdsToRemindForValidation(any(), any())).thenReturn(List.of(1L, 2L, 3L));
        when(validationReminderService.sendReminder(1L)).thenReturn(true);
        when(validationReminderService.sendReminder(2L)).thenThrow(new IllegalStateException("boom"));
        when(validationReminderService.sendReminder(3L)).thenReturn(true);

        task.sendValidationReminders();

        verify(validationReminderService).sendReminder(1L);
        verify(validationReminderService).sendReminder(2L);
        verify(validationReminderService).sendReminder(3L);
    }
}
