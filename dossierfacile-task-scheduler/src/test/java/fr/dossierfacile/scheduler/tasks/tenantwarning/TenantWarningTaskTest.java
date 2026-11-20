package fr.dossierfacile.scheduler.tasks.tenantwarning;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.logging.task.LogAggregator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantWarningTaskTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantWarningService tenantWarningService;
    @Mock
    private TenantArchivingService tenantArchivingService;

    // In-memory tenants with documents, mutated by the mocked services like the database would be
    private final Map<Long, Tenant> tenantsWithDocuments = new TreeMap<>();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger taskLogger = (Logger) LoggerFactory.getLogger(TenantWarningTask.class);

    private TenantWarningTask task;

    @BeforeEach
    void setUp() {
        task = new TenantWarningTask(tenantRepository, tenantWarningService, tenantArchivingService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };
        ReflectionTestUtils.setField(task, "daysForDeletionOfDocuments", 45);
        ReflectionTestUtils.setField(task, "daysForFirstWarningDeletion", 30);
        ReflectionTestUtils.setField(task, "daysForSecondWarningDeletion", 37);

        when(tenantRepository.findInactiveTenantsWithDocuments(any(), anyInt(), anyLong(), any()))
                .thenAnswer(invocation -> batch(invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3)));
        when(tenantRepository.countInactiveTenantsWithDocuments(any(), anyInt()))
                .thenAnswer(invocation -> tenantsWithDocuments.values().stream()
                        .filter(t -> t.getWarnings() == (int) invocation.getArgument(1)).count());
        when(tenantRepository.findInactiveTenantsWithoutDocuments(any(), anyLong(), any())).thenReturn(List.of());

        // Warning services increment the counter, archiving removes the documents
        doAnswer(invocation -> setWarnings(invocation.getArgument(0), 1))
                .when(tenantWarningService).sendFirstWarning(any());
        doAnswer(invocation -> setWarnings(invocation.getArgument(0), 2))
                .when(tenantWarningService).sendSecondWarning(any());
        doAnswer(invocation -> tenantsWithDocuments.remove(((Tenant) invocation.getArgument(0)).getId()))
                .when(tenantArchivingService).archiveTenant(any());

        logs.start();
        taskLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        taskLogger.detachAppender(logs);
    }

    @Test
    void should_warn_every_eligible_tenant_even_when_processed_tenants_leave_the_result_set() {
        addTenants(1, 250, 0);

        task.accountWarningsForDocumentDeletion();

        verify(tenantWarningService, times(250)).sendFirstWarning(any());
        assertThat(tenantsWithDocuments.values()).allMatch(t -> t.getWarnings() == 1);
    }

    @Test
    void should_archive_every_eligible_tenant_even_when_processed_tenants_leave_the_result_set() {
        addTenants(1, 250, 2);

        task.accountWarningsForDocumentDeletion();

        verify(tenantArchivingService, times(250)).archiveTenant(any());
        assertThat(tenantsWithDocuments).isEmpty();
    }

    @Test
    void should_retry_on_next_run_without_alerting_when_a_brevo_failure_is_isolated() {
        addTenants(1, 10, 0);
        failTransiently(Set.of(5L));

        task.accountWarningsForDocumentDeletion();

        verify(tenantWarningService, times(10)).sendFirstWarning(any());
        assertThat(tenantsWithDocuments.get(5L).getWarnings()).isZero();
        assertThat(tenantsWithDocuments.values()).filteredOn(t -> t.getId() != 5L).allMatch(t -> t.getWarnings() == 1);
        assertThat(logsAt(Level.ERROR)).isEmpty();
        assertThat(logsAt(Level.WARN)).anyMatch(message -> message.contains("tenant [5]"));

        // Next run: Brevo is back, only the tenant left behind is warned
        failTransiently(Set.of());
        task.accountWarningsForDocumentDeletion();

        verify(tenantWarningService, times(11)).sendFirstWarning(any());
        assertThat(tenantsWithDocuments.get(5L).getWarnings()).isEqualTo(1);
    }

    @Test
    void should_alert_when_every_email_fails_because_brevo_looks_down() {
        addTenants(1, 50, 0);
        failTransiently(Set.copyOf(tenantsWithDocuments.keySet()));

        task.accountWarningsForDocumentDeletion();

        // Every tenant is still attempted and none is marked as warned: they are all retried on the next run
        verify(tenantWarningService, times(50)).sendFirstWarning(any());
        assertThat(tenantsWithDocuments.values()).allMatch(t -> t.getWarnings() == 0);
        assertThat(logsAt(Level.ERROR)).singleElement().asString().contains("all 50 warning emails failed");
    }

    @Test
    void should_still_alert_on_unexpected_errors() {
        addTenants(1, 3, 0);
        doAnswer(invocation -> {
            throw new IllegalStateException("boom");
        }).when(tenantWarningService).sendFirstWarning(any());

        task.accountWarningsForDocumentDeletion();

        assertThat(logsAt(Level.ERROR)).hasSize(3);
    }

    private List<Tenant> batch(int warnings, long beforeId, Pageable limit) {
        return tenantsWithDocuments.values().stream()
                .filter(t -> t.getWarnings() == warnings && t.getId() < beforeId)
                .sorted(Comparator.comparing(Tenant::getId).reversed())
                .limit(limit.getPageSize())
                .toList();
    }

    private void addTenants(long fromId, long toId, int warnings) {
        LongStream.rangeClosed(fromId, toId).forEach(id -> {
            Tenant tenant = new Tenant();
            tenant.setId(id);
            tenant.setWarnings(warnings);
            tenantsWithDocuments.put(id, tenant);
        });
    }

    private Object setWarnings(Tenant tenant, int warnings) {
        tenantsWithDocuments.get(tenant.getId()).setWarnings(warnings);
        return null;
    }

    private void failTransiently(Set<Long> tenantIds) {
        doAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            if (tenantIds.contains(tenant.getId())) {
                throw new TransientMailDeliveryException("timeout", 0, null);
            }
            return setWarnings(tenant, 1);
        }).when(tenantWarningService).sendFirstWarning(any());
    }

    private List<String> logsAt(Level level) {
        return logs.list.stream()
                .filter(event -> event.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
