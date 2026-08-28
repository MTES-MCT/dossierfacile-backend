package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.LotteryPublicStatus;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LotteryTicketServiceImplTest {

    private static final Long TENANT_ID = 100L;

    @Mock
    private LotteryTicketRepository lotteryTicketRepository;
    @Mock
    private TenantLogCommonService tenantLogCommonService;
    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private LotteryTicketServiceImpl lotteryTicketService;

    private final Tenant tenant = Tenant.builder().id(TENANT_ID).build();

    private LotteryTicket ticket(LotteryTicketStatus status) {
        return LotteryTicket.builder().id(1L).tenantId(TENANT_ID).status(status).build();
    }

    private void mockActiveTicket(LotteryTicket ticket) {
        when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(
                eq(TENANT_ID), eq(Set.of(LotteryTicketStatus.PENDING, LotteryTicketStatus.DRAWN))))
                .thenReturn(Optional.ofNullable(ticket));
    }

    private void mockLastNotDrawn(LocalDate cooldownUntil) {
        LotteryTicket notDrawn = ticket(LotteryTicketStatus.NOT_DRAWN);
        notDrawn.setCooldownUntil(cooldownUntil);
        when(lotteryTicketRepository.findFirstByTenantIdAndStatusOrderByIdDesc(TENANT_ID, LotteryTicketStatus.NOT_DRAWN))
                .thenReturn(Optional.of(notDrawn));
    }

    @Nested
    class Apply {

        @Test
        void should_create_a_pending_ticket() {
            mockActiveTicket(null);
            when(lotteryTicketRepository.findFirstByTenantIdAndStatusOrderByIdDesc(TENANT_ID, LotteryTicketStatus.NOT_DRAWN))
                    .thenReturn(Optional.empty());
            when(lotteryTicketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            LotteryTicket created = lotteryTicketService.apply(tenant);

            assertThat(created.getStatus()).isEqualTo(LotteryTicketStatus.PENDING);
            assertThat(created.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        void should_be_idempotent_when_an_active_ticket_exists() {
            LotteryTicket pending = ticket(LotteryTicketStatus.PENDING);
            mockActiveTicket(pending);

            assertThat(lotteryTicketService.apply(tenant)).isSameAs(pending);
            verify(lotteryTicketRepository, never()).save(any());
        }

        @Test
        void should_refuse_during_cooldown() {
            mockActiveTicket(null);
            mockLastNotDrawn(LocalDate.now().plusDays(2));

            assertThatThrownBy(() -> lotteryTicketService.apply(tenant))
                    .isInstanceOf(IllegalStateException.class);
            verify(lotteryTicketRepository, never()).save(any());
        }

        @Test
        void should_allow_a_new_application_once_the_cooldown_has_ended() {
            mockActiveTicket(null);
            mockLastNotDrawn(LocalDate.now());
            when(lotteryTicketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(lotteryTicketService.apply(tenant).getStatus()).isEqualTo(LotteryTicketStatus.PENDING);
        }
    }

    @Nested
    class Cancel {

        @Test
        void should_cancel_a_pending_ticket_and_log() {
            LotteryTicket pending = ticket(LotteryTicketStatus.PENDING);
            mockActiveTicket(pending);

            lotteryTicketService.cancelActiveTicket(tenant);

            assertThat(pending.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            verify(tenantLogCommonService).saveTenantLog(argThat((TenantLog log) ->
                    log.getLogType() == LogType.LOTTERY_TICKET_CANCELLED && log.getTenantId().equals(TENANT_ID)));
        }

        @Test
        void should_cancel_an_active_ticket_on_tenant_withdrawal() {
            LotteryTicket drawn = ticket(LotteryTicketStatus.DRAWN);
            mockActiveTicket(drawn);

            lotteryTicketService.cancelActiveTicket(tenant);

            assertThat(drawn.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
        }
    }

    @Nested
    class Consume {

        @Test
        void should_consume_a_drawn_ticket() {
            LotteryTicket drawn = ticket(LotteryTicketStatus.DRAWN);
            when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(TENANT_ID, Set.of(LotteryTicketStatus.DRAWN)))
                    .thenReturn(Optional.of(drawn));

            lotteryTicketService.consumeDrawnTicket(TENANT_ID);

            assertThat(drawn.getStatus()).isEqualTo(LotteryTicketStatus.CONSUMED);
        }

        @Test
        void should_be_a_no_op_without_a_drawn_ticket() {
            when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(TENANT_ID, Set.of(LotteryTicketStatus.DRAWN)))
                    .thenReturn(Optional.empty());

            lotteryTicketService.consumeDrawnTicket(TENANT_ID);

            verify(lotteryTicketRepository, never()).save(any());
        }
    }

    @Nested
    class PublicStatus {

        @Test
        void should_be_empty_when_the_lottery_flag_is_off() {
            when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(false);

            assertThat(lotteryTicketService.getPublicStatus(TENANT_ID)).isEmpty();
            verify(lotteryTicketRepository, never()).findFirstByTenantIdAndStatusIn(anyLong(), anyCollection());
        }

        @Test
        void should_expose_pending_then_drawn_then_cooldown() {
            when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);

            mockActiveTicket(ticket(LotteryTicketStatus.PENDING));
            assertThat(lotteryTicketService.getPublicStatus(TENANT_ID))
                    .hasValueSatisfying(view -> assertThat(view.status()).isEqualTo(LotteryPublicStatus.PENDING));

            mockActiveTicket(ticket(LotteryTicketStatus.DRAWN));
            assertThat(lotteryTicketService.getPublicStatus(TENANT_ID))
                    .hasValueSatisfying(view -> assertThat(view.status()).isEqualTo(LotteryPublicStatus.DRAWN));

            mockActiveTicket(null);
            LocalDate endDate = LocalDate.now().plusDays(1);
            mockLastNotDrawn(endDate);
            assertThat(lotteryTicketService.getPublicStatus(TENANT_ID)).hasValueSatisfying(view -> {
                assertThat(view.status()).isEqualTo(LotteryPublicStatus.COOLDOWN);
                assertThat(view.nextEligibleDate()).isEqualTo(endDate);
            });
        }

        @Test
        void should_be_empty_without_active_ticket_nor_cooldown() {
            when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);
            mockActiveTicket(null);
            when(lotteryTicketRepository.findFirstByTenantIdAndStatusOrderByIdDesc(TENANT_ID, LotteryTicketStatus.NOT_DRAWN))
                    .thenReturn(Optional.empty());

            assertThat(lotteryTicketService.getPublicStatus(TENANT_ID)).isEmpty();
        }
    }
}
