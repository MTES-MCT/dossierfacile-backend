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
class CompletedSurveyTaskTest {

    @Mock
    private CompletedOptinTenantRepository tenantRepository;
    @Mock
    private CompletedSurveyService completedSurveyService;

    private CompletedSurveyTask task;

    @BeforeEach
    void setUp() {
        // Initialize the task manually to override the @Lookup method
        task = new CompletedSurveyTask(tenantRepository, completedSurveyService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };
        ReflectionTestUtils.setField(task, "minAgeDays", 42L);
        ReflectionTestUtils.setField(task, "maxAgeDays", 45L);
    }

    @Test
    void should_select_the_tenants_submitted_between_45_and_42_days_ago() {
        when(tenantRepository.findTenantIdsForCompletedSurvey(any(), any())).thenReturn(List.of());

        task.sendCompletedSurveys();

        ArgumentCaptor<LocalDateTime> submittedFrom = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> submittedBefore = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tenantRepository).findTenantIdsForCompletedSurvey(submittedFrom.capture(), submittedBefore.capture());
        assertThat(Duration.between(submittedFrom.getValue(), submittedBefore.getValue())).isEqualTo(Duration.ofDays(3));
        assertThat(Duration.between(submittedBefore.getValue(), LocalDateTime.now()))
                .isBetween(Duration.ofDays(42), Duration.ofDays(42).plusMinutes(1));
    }

    @Test
    void should_keep_surveying_the_other_tenants_when_one_fails() {
        when(tenantRepository.findTenantIdsForCompletedSurvey(any(), any())).thenReturn(List.of(1L, 2L, 3L));
        when(completedSurveyService.sendSurvey(1L)).thenReturn(true);
        when(completedSurveyService.sendSurvey(2L)).thenThrow(new IllegalStateException("boom"));
        when(completedSurveyService.sendSurvey(3L)).thenReturn(true);

        task.sendCompletedSurveys();

        verify(completedSurveyService).sendSurvey(1L);
        verify(completedSurveyService).sendSurvey(2L);
        verify(completedSurveyService).sendSurvey(3L);
    }
}
