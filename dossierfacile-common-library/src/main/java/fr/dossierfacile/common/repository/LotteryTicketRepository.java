package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LotteryTicketRepository extends JpaRepository<LotteryTicket, Long> {

    Optional<LotteryTicket> findFirstByTenantIdAndStatusIn(Long tenantId, Collection<LotteryTicketStatus> statuses);

    Optional<LotteryTicket> findFirstByTenantIdAndStatusOrderByIdDesc(Long tenantId, LotteryTicketStatus status);

    List<LotteryTicket> findAllByStatus(LotteryTicketStatus status);

    /**
     * Tickets in the draw: PENDING on a dossier in the draw scope, random order —
     * the first rows win.
     */
    @Query(value = """
            SELECT e.*
            FROM lottery_ticket e
              JOIN tenant t ON t.id = e.tenant_id
              JOIN apartment_sharing a ON a.id = t.apartment_sharing_id
            WHERE e.status = 'PENDING'
              AND t.status = 'COMPLETED'
              AND a.application_type = 'ALONE'
              AND NOT EXISTS (SELECT 1 FROM tenant_userapi tu WHERE tu.tenant_id = t.id)
            ORDER BY random()
            """, nativeQuery = true)
    List<LotteryTicket> findDrawTickets();

    /**
     * PENDING tickets whose dossier is out of the draw scope at draw time (not
     * COMPLETED, no longer ALONE, or linked to a partner): the draw cancels them
     * (no cooldown). Complement of {@link #findDrawTickets()}.
     */
    @Query(value = """
            SELECT e.*
            FROM lottery_ticket e
              JOIN tenant t ON t.id = e.tenant_id
              JOIN apartment_sharing a ON a.id = t.apartment_sharing_id
            WHERE e.status = 'PENDING'
              AND (t.status <> 'COMPLETED'
                   OR a.application_type <> 'ALONE'
                   OR EXISTS (SELECT 1 FROM tenant_userapi tu WHERE tu.tenant_id = t.id))
            """, nativeQuery = true)
    List<LotteryTicket> findPendingOutOfDrawScope();

    List<LotteryTicket> findAllByStatusAndCooldownUntilLessThanEqualAndCooldownNotifiedAtIsNull(
            LotteryTicketStatus status, LocalDate maxCooldownUntil);

    /**
     * Grandfathering on flag activation: opt-ins already waiting in the queue (no
     * lottery involved) get a DRAWN ticket so the "in queue via opt-in <=> DRAWN
     * ticket" invariant holds and no status recomputation ejects them. Idempotent:
     * tenants holding an active ticket are skipped. Returns the inserted count.
     */
    @Modifying
    @Query(value = """
            INSERT INTO lottery_ticket (tenant_id, status, created_at, drawn_at)
            SELECT t.id, 'DRAWN', now(), now()
            FROM tenant t
            WHERE t.status = 'TO_PROCESS'
              AND t.validation_requested = true
              AND NOT EXISTS (SELECT 1 FROM lottery_ticket lt
                              WHERE lt.tenant_id = t.id AND lt.status IN ('PENDING', 'DRAWN'))
            """, nativeQuery = true)
    int grantDrawnTicketsToQueuedOptIns();
}
