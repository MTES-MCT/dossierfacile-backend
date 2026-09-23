package fr.dossierfacile.scheduler.cli;

import fr.dossierfacile.logging.task.LogAggregator;
import fr.dossierfacile.scheduler.tasks.analytics.AnalyticsProperties;
import fr.dossierfacile.scheduler.tasks.analytics.AnalyticsReplicationService;
import fr.dossierfacile.scheduler.tasks.analytics.DbtTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsReplicationRunnerTest {

    @Mock
    private AnalyticsReplicationService replicationService;

    @Mock
    private DbtTriggerService dbtTriggerService;

    @Mock
    private LogAggregator logAggregator;

    private AnalyticsProperties properties;
    private AnalyticsReplicationRunner runner;
    private AtomicInteger exitCode;

    @BeforeEach
    void setUp() {
        properties = new AnalyticsProperties();
        properties.setEnabled(true);
        runner = new AnalyticsReplicationRunner(properties, replicationService, dbtTriggerService, logAggregator);
        exitCode = new AtomicInteger(-1);
        runner.setExitHandler(exitCode::set);
    }

    @Test
    @DisplayName("Ne doit rien exécuter si l'argument --run-task=replicate-analytics est absent")
    void should_not_run_when_argument_missing() {
        runner.run("--other-arg");

        verifyNoInteractions(replicationService);
        verifyNoInteractions(dbtTriggerService);
        verifyNoInteractions(logAggregator);
        assertThat(exitCode.get()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Doit ignorer la tâche et quitter avec exit 0 si analytics.replication.enabled est false")
    void should_skip_task_when_disabled_and_exit_0() {
        properties.setEnabled(false);

        runner.run("--run-task=replicate-analytics");

        verifyNoInteractions(replicationService);
        verifyNoInteractions(dbtTriggerService);
        verifyNoInteractions(logAggregator);
        assertThat(exitCode.get()).isZero();
    }

    @Test
    @DisplayName("Doit exécuter la réplication, déclencher dbt, envoyer les logs et terminer avec exit 0")
    void should_run_successfully_and_exit_0() throws SQLException {
        runner.run("--run-task=replicate-analytics");

        verify(replicationService).replicateAll();
        verify(dbtTriggerService).trigger();
        verify(logAggregator).sendLogs();
        assertThat(exitCode.get()).isZero();
    }

    @Test
    @DisplayName("Doit déclencher dbt, envoyer les logs et terminer avec exit 1 en cas d'erreur de réplication")
    void should_trigger_dbt_log_and_exit_1_on_failure() throws SQLException {
        doThrow(new SQLException("Connection refused")).when(replicationService).replicateAll();

        runner.run("--run-task=replicate-analytics");

        verify(replicationService).replicateAll();
        verify(dbtTriggerService).trigger();
        verify(logAggregator).sendLogs();
        assertThat(exitCode.get()).isEqualTo(1);
    }
}
