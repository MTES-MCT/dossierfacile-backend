package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLogCommonServiceImplTest {

    private static final Long TENANT_ID = 42L;

    @Mock
    private TenantLogRepository tenantLogRepository;
    @Mock
    private LotteryTicketRepository lotteryTicketRepository;

    private TenantLog logQueueEntered(QueueEntrySource source) {
        TenantLogCommonServiceImpl service = new TenantLogCommonServiceImpl(
                tenantLogRepository, lotteryTicketRepository, new ObjectMapper());
        service.logQueueEntered(TENANT_ID, source);
        ArgumentCaptor<TenantLog> captor = ArgumentCaptor.forClass(TenantLog.class);
        verify(tenantLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_flag_the_entry_as_bypass_without_a_drawn_ticket() {
        when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(eq(TENANT_ID), eq(Set.of(LotteryTicketStatus.DRAWN))))
                .thenReturn(Optional.empty());

        TenantLog log = logQueueEntered(QueueEntrySource.SUBMISSION);

        assertThat(log.getLogType()).isEqualTo(LogType.QUEUE_ENTERED);
        assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(log.getLogDetails().get("source").asText()).isEqualTo("SUBMISSION");
        assertThat(log.getLogDetails().get("bypass").asBoolean()).isTrue();
    }

    @Test
    void should_not_flag_the_entry_as_bypass_with_a_drawn_ticket() {
        when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(eq(TENANT_ID), eq(Set.of(LotteryTicketStatus.DRAWN))))
                .thenReturn(Optional.of(LotteryTicket.builder().tenantId(TENANT_ID).status(LotteryTicketStatus.DRAWN).build()));

        TenantLog log = logQueueEntered(QueueEntrySource.LOTTERY_DRAW);

        assertThat(log.getLogDetails().get("source").asText()).isEqualTo("LOTTERY_DRAW");
        assertThat(log.getLogDetails().get("bypass").asBoolean()).isFalse();
    }
}
