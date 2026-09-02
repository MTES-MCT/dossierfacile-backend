package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.LotteryDraw;
import fr.dossierfacile.common.entity.ProcessingCapacity;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.LotteryDrawRepository;
import fr.dossierfacile.common.repository.ProcessingCapacityRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LotteryDrawServiceImplTest {

    private static final LocalDate DRAW_DATE = LocalDate.of(2026, 9, 15);

    private FeatureFlagService featureFlagService;
    private ProcessingCapacityRepository processingCapacityRepository;
    private LotteryDrawRepository lotteryDrawRepository;
    private LotteryTicketRepository lotteryTicketRepository;
    private LotteryTicketServiceImpl lotteryTicketService;
    private TenantUserApiRepository tenantUserApiRepository;
    private OperatorReviewPolicyImpl operatorReviewPolicy;
    private TenantLogRepository tenantLogRepository;
    private TenantCommonRepository tenantCommonRepository;
    private TenantLogCommonService tenantLogCommonService;
    private ApartmentSharingCommonService apartmentSharingCommonService;
    private TenantMapperForMail tenantMapperForMail;
    private MailCommonService mailCommonService;

    private LotteryDrawServiceImpl service;

    private final AtomicLong idSequence = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        featureFlagService = mock(FeatureFlagService.class);
        processingCapacityRepository = mock(ProcessingCapacityRepository.class);
        lotteryDrawRepository = mock(LotteryDrawRepository.class);
        lotteryTicketRepository = mock(LotteryTicketRepository.class);
        tenantLogRepository = mock(TenantLogRepository.class);
        tenantCommonRepository = mock(TenantCommonRepository.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);
        tenantMapperForMail = mock(TenantMapperForMail.class);
        mailCommonService = mock(MailCommonService.class);
        lotteryTicketService = new LotteryTicketServiceImpl(
                lotteryTicketRepository, tenantLogCommonService, featureFlagService);
        tenantUserApiRepository = mock(TenantUserApiRepository.class);
        operatorReviewPolicy = new OperatorReviewPolicyImpl(
                tenantUserApiRepository, featureFlagService, lotteryTicketRepository);

        service = new LotteryDrawServiceImpl(
                featureFlagService,
                processingCapacityRepository,
                lotteryDrawRepository,
                lotteryTicketRepository,
                lotteryTicketService,
                operatorReviewPolicy,
                tenantLogRepository,
                tenantCommonRepository,
                tenantLogCommonService,
                apartmentSharingCommonService,
                tenantMapperForMail,
                Optional.of(mailCommonService)
        );
        // Without Spring, the transactional self-proxy is the service itself
        ReflectionTestUtils.setField(service, "self", service);

        when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);
        // Every tenant is in the opt-in rollout unless a test says otherwise
        when(featureFlagService.isFeatureEnabledForUser(anyLong(), eq(OperatorReviewPolicy.COMPLETED_OPTIN_FEATURE_FLAG)))
                .thenReturn(true);
        when(lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(DRAW_DATE)).thenReturn(Optional.empty());
        when(lotteryTicketRepository.findPendingOutOfDrawScope()).thenReturn(List.of());
        when(lotteryDrawRepository.save(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            if (draw.getId() == null) {
                draw.setId(idSequence.incrementAndGet());
            }
            when(lotteryDrawRepository.findById(draw.getId())).thenReturn(Optional.of(draw));
            return draw;
        });
        // afterCommit callbacks require an active synchronization
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void mockCapacity(int dailyCount) {
        when(processingCapacityRepository.findByDate(DRAW_DATE)).thenReturn(
                ProcessingCapacity.builder().id(1L).date(DRAW_DATE).dailyCount(dailyCount).build());
    }

    private void mockBypass(long count) {
        when(tenantLogRepository.countBypassQueueEntries(any(), any())).thenReturn(count);
    }

    private Tenant tenant(Long id, TenantFileStatus status) {
        return tenant(id, status, ApplicationType.ALONE);
    }

    private Tenant tenant(Long id, TenantFileStatus status, ApplicationType applicationType) {
        Tenant tenant = Tenant.builder().id(id).status(status)
                .apartmentSharing(ApartmentSharing.builder().id(id + 500).applicationType(applicationType).build()).build();
        when(tenantCommonRepository.findById(id)).thenReturn(Optional.of(tenant));
        return tenant;
    }

    /** The SQL lists the pool, then the dossier or ticket changes before its own transaction. */
    private void listedThenChanged(List<LotteryTicket> pool, Runnable change) {
        when(lotteryTicketRepository.findDrawTickets()).thenAnswer(invocation -> {
            change.run();
            return pool;
        });
    }

    private LotteryTicket pendingTicket(Long id, Long tenantId) {
        return ticket(id, tenantId, LotteryTicketStatus.PENDING);
    }

    private LotteryTicket ticket(Long id, Long tenantId, LotteryTicketStatus status) {
        LotteryTicket ticket = LotteryTicket.builder().id(id).tenantId(tenantId).status(status).build();
        when(lotteryTicketRepository.findById(id)).thenReturn(Optional.of(ticket));
        return ticket;
    }

    @Nested
    class ExecuteDraw {
        @Test
        void should_skip_when_the_flag_is_off() {
            when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(false);

            assertThat(service.executeDrawIfNeeded(DRAW_DATE)).isEmpty();
            verify(lotteryDrawRepository, never()).save(any());
        }

        @Test
        void should_return_the_existing_draw_when_already_executed_today() {
            LotteryDraw existingDraw = LotteryDraw.builder().id(1L).drawDate(DRAW_DATE).build();
            when(lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(DRAW_DATE)).thenReturn(Optional.of(existingDraw));

            assertThat(service.executeDrawIfNeeded(DRAW_DATE)).contains(existingDraw);
            verify(lotteryDrawRepository, never()).save(any());
        }

        @Test
        void should_run_a_second_draw_the_same_day_when_multiple_draws_are_allowed() {
            ReflectionTestUtils.setField(service, "allowMultipleDrawsPerDay", true);
            LotteryDraw firstDraw = LotteryDraw.builder().id(1L).drawDate(DRAW_DATE).drawnCount(3).build();
            when(lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(DRAW_DATE)).thenReturn(Optional.of(firstDraw));
            mockCapacity(10);
            mockBypass(0);
            LotteryTicket ticket = pendingTicket(1L, 100L);
            tenant(100L, TenantFileStatus.COMPLETED);
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of(ticket));

            Optional<LotteryDraw> secondDraw = service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(secondDraw).isPresent();
            assertThat(secondDraw.get().getId()).isNotEqualTo(firstDraw.getId());
            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.DRAWN);
        }

        @Test
        void should_not_record_a_draw_when_no_capacity_is_defined() {
            when(processingCapacityRepository.findByDate(DRAW_DATE)).thenReturn(null);

            assertThat(service.executeDrawIfNeeded(DRAW_DATE)).isEmpty();
            verify(lotteryDrawRepository, never()).save(any());
            verify(lotteryTicketRepository, never()).save(any());
        }

        @Test
        void should_keep_tickets_pending_when_bypass_exceeds_capacity() {
            mockCapacity(5);
            mockBypass(8);
            LotteryTicket ticket = pendingTicket(1L, 100L);
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of(ticket));

            Optional<LotteryDraw> draw = service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(draw).hasValueSatisfying(r -> {
                assertThat(r.getAvailableSlots()).isEqualTo(-3);
                assertThat(r.getDrawnCount()).isZero();
                assertThat(r.getTicketCount()).isEqualTo(1);
            });
            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.PENDING);
        }

        @Test
        void should_fill_the_available_slots_and_put_the_others_in_cooldown() {
            mockCapacity(10);
            mockBypass(9); // 1 slot
            LotteryTicket winner = pendingTicket(1L, 100L);
            LotteryTicket loser = pendingTicket(2L, 200L);
            Tenant winnerTenant = tenant(100L, TenantFileStatus.COMPLETED);
            tenant(200L, TenantFileStatus.COMPLETED);
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of(winner, loser));

            Optional<LotteryDraw> draw = service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(winner.getStatus()).isEqualTo(LotteryTicketStatus.DRAWN);
            assertThat(winnerTenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            assertThat(winnerTenant.getLastUpdateDate()).isNotNull();
            assertThat(loser.getStatus()).isEqualTo(LotteryTicketStatus.NOT_DRAWN);
            assertThat(loser.getCooldownUntil()).isEqualTo(DRAW_DATE.plusDays(LotteryTicketService.COOLDOWN_DAYS));
            assertThat(draw).hasValueSatisfying(r -> assertThat(r.getDrawnCount()).isEqualTo(1));
            verify(tenantLogCommonService).logQueueEntered(100L, QueueEntrySource.LOTTERY_DRAW);
            verify(tenantLogCommonService, never()).logQueueEntered(eq(200L), any());
            verify(tenantCommonRepository).refreshRank();
        }


        @Test
        void should_cancel_when_the_dossier_is_no_longer_completed() {
            mockCapacity(10);
            mockBypass(0);
            LotteryTicket outOfScope = pendingTicket(1L, 100L);
            tenant(100L, TenantFileStatus.INCOMPLETE);
            when(lotteryTicketRepository.findPendingOutOfDrawScope()).thenReturn(List.of(outOfScope));
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of());

            service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(outOfScope.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(outOfScope.getCooldownUntil()).isNull();
        }

        @Test
        void should_cancel_when_the_dossier_is_no_longer_alone() {
            mockCapacity(10);
            mockBypass(0);
            LotteryTicket ticket = pendingTicket(1L, 100L);
            Tenant coupleTenant = tenant(100L, TenantFileStatus.COMPLETED, ApplicationType.COUPLE);
            when(lotteryTicketRepository.findPendingOutOfDrawScope()).thenReturn(List.of(ticket));
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of());

            service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(ticket.getCooldownUntil()).isNull();
            assertThat(coupleTenant.getStatus()).isEqualTo(TenantFileStatus.COMPLETED);
        }

        @Test
        void should_cancel_when_no_slot_is_available() {
            mockCapacity(5);
            mockBypass(8); // no slot
            LotteryTicket outOfScope = pendingTicket(1L, 100L);
            tenant(100L, TenantFileStatus.INCOMPLETE);
            when(lotteryTicketRepository.findPendingOutOfDrawScope()).thenReturn(List.of(outOfScope));
            when(lotteryTicketRepository.findDrawTickets()).thenReturn(List.of());

            service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(outOfScope.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
        }

        // Race condition: the pool is listed by SQL, then each ticket is re-read in its
        // own transaction. A dossier that changed in between is cancelled without
        // cooldown, the slot is lost. listedThenChanged() simulates the change right
        // after the listing.

        @Test
        void should_cancel_when_the_dossier_left_completed_during_the_draw() {
            mockCapacity(10);
            mockBypass(0);
            LotteryTicket ticket = pendingTicket(1L, 100L);
            Tenant tenant = tenant(100L, TenantFileStatus.COMPLETED);
            listedThenChanged(List.of(ticket), () -> tenant.setStatus(TenantFileStatus.INCOMPLETE));

            Optional<LotteryDraw> draw = service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(ticket.getCooldownUntil()).isNull();
            assertThat(draw).hasValueSatisfying(r -> assertThat(r.getDrawnCount()).isZero());
        }

        @Test
        void should_cancel_when_a_partner_was_linked_during_the_draw() {
            mockCapacity(10);
            mockBypass(0);
            LotteryTicket ticket = pendingTicket(1L, 100L);
            Tenant tenant = tenant(100L, TenantFileStatus.COMPLETED);
            listedThenChanged(List.of(ticket), () -> when(tenantUserApiRepository.existsByTenant(tenant)).thenReturn(true));

            Optional<LotteryDraw> draw = service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(ticket.getCooldownUntil()).isNull();
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.COMPLETED);
            assertThat(draw).hasValueSatisfying(r -> assertThat(r.getDrawnCount()).isZero());
            verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
        }

        @Test
        void should_cancel_when_the_dossier_became_a_couple_during_the_draw() {
            mockCapacity(10);
            mockBypass(9); // 1 slot: the second ticket goes through markNotDrawn
            LotteryTicket winner = pendingTicket(1L, 100L);
            LotteryTicket coupleTicket = pendingTicket(2L, 200L);
            tenant(100L, TenantFileStatus.COMPLETED);
            Tenant tenant = tenant(200L, TenantFileStatus.COMPLETED);
            listedThenChanged(List.of(winner, coupleTicket),
                    () -> tenant.getApartmentSharing().setApplicationType(ApplicationType.COUPLE));

            service.executeDrawIfNeeded(DRAW_DATE);

            assertThat(winner.getStatus()).isEqualTo(LotteryTicketStatus.DRAWN);
            assertThat(coupleTicket.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(coupleTicket.getCooldownUntil()).isNull();
        }

        @Test
        void should_not_override_a_cancellation_made_during_the_draw() {
            mockCapacity(10);
            mockBypass(9); // 1 slot
            LotteryTicket cancelledByTenant = pendingTicket(1L, 100L);
            LotteryTicket next = pendingTicket(2L, 200L);
            tenant(200L, TenantFileStatus.COMPLETED);
            listedThenChanged(List.of(cancelledByTenant, next),
                    () -> cancelledByTenant.setStatus(LotteryTicketStatus.CANCELLED));

            Optional<LotteryDraw> draw = service.executeDrawIfNeeded(DRAW_DATE);

            // The tenant's decision stands, its dossier is not even loaded
            assertThat(cancelledByTenant.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            verify(tenantCommonRepository, never()).findById(100L);
            verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
            // The slot is lost, not handed to the next ticket
            assertThat(next.getStatus()).isEqualTo(LotteryTicketStatus.NOT_DRAWN);
            assertThat(draw).hasValueSatisfying(r -> assertThat(r.getDrawnCount()).isZero());
        }
    }

    @Nested
    class NotifyCooldownEnded {

        @Test
        void should_skip_when_the_flag_is_off() {
            when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(false);

            assertThat(service.notifyCooldownEnded(DRAW_DATE)).isZero();
        }

        @Test
        void should_notify_once_and_only_completed_dossiers() {
            LotteryTicket notifiable = LotteryTicket.builder().id(1L).tenantId(100L)
                    .status(LotteryTicketStatus.NOT_DRAWN).cooldownUntil(DRAW_DATE.minusDays(1)).build();
            when(lotteryTicketRepository.findById(1L)).thenReturn(Optional.of(notifiable));
            tenant(100L, TenantFileStatus.COMPLETED);
            LotteryTicket leftCompleted = LotteryTicket.builder().id(2L).tenantId(200L)
                    .status(LotteryTicketStatus.NOT_DRAWN).cooldownUntil(DRAW_DATE.minusDays(1)).build();
            when(lotteryTicketRepository.findById(2L)).thenReturn(Optional.of(leftCompleted));
            tenant(200L, TenantFileStatus.VALIDATED);
            when(lotteryTicketRepository.findAllByStatusAndCooldownUntilLessThanEqualAndCooldownNotifiedAtIsNull(
                    LotteryTicketStatus.NOT_DRAWN, DRAW_DATE)).thenReturn(List.of(notifiable, leftCompleted));

            int notified = service.notifyCooldownEnded(DRAW_DATE);

            assertThat(notified).isEqualTo(1);
            // Both are marked notified so the batch never retries them
            assertThat(notifiable.getCooldownNotifiedAt()).isNotNull();
            assertThat(leftCompleted.getCooldownNotifiedAt()).isNotNull();
        }
    }

    @Nested
    class Flush {

        @Test
        void should_process_pending_dossiers() {
            LotteryTicket ticket = pendingTicket(1L, 100L);
            Tenant completedTenant = tenant(100L, TenantFileStatus.COMPLETED);
            when(lotteryTicketRepository.findAllByStatus(LotteryTicketStatus.PENDING)).thenReturn(List.of(ticket));

            int flushed = service.flushPendingTicketsToProcessing();

            assertThat(flushed).isEqualTo(1);
            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.DRAWN);
            assertThat(ticket.getLotteryDrawId()).isNull();
            assertThat(completedTenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            verify(tenantLogCommonService).logQueueEntered(100L, QueueEntrySource.LOTTERY_DRAW);
            verify(tenantCommonRepository).refreshRank();
        }

        @Test
        void should_cancel_pending_applications_on_non_completed_dossiers() {
            LotteryTicket ticket = pendingTicket(1L, 100L);
            Tenant incompleteTenant = tenant(100L, TenantFileStatus.INCOMPLETE);
            when(lotteryTicketRepository.findAllByStatus(LotteryTicketStatus.PENDING)).thenReturn(List.of(ticket));

            int flushed = service.flushPendingTicketsToProcessing();

            assertThat(flushed).isZero();
            assertThat(ticket.getStatus()).isEqualTo(LotteryTicketStatus.CANCELLED);
            assertThat(incompleteTenant.getStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
            verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
        }
    }

    @Nested
    class GrantTicketsToQueuedOptIns {

        @Test
        void should_grant_tickets_through_the_repository_without_touching_the_dossiers() {
            when(lotteryTicketRepository.grantDrawnTicketsToQueuedOptIns()).thenReturn(3);

            int granted = service.grantTicketsToQueuedOptIns();

            assertThat(granted).isEqualTo(3);
            verify(lotteryTicketRepository).grantDrawnTicketsToQueuedOptIns();
            // Already TO_PROCESS: no queue entry, no rank refresh
            verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
            verify(tenantCommonRepository, never()).refreshRank();
        }
    }
}
