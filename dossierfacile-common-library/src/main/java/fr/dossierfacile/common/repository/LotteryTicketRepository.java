package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LotteryTicketRepository extends JpaRepository<LotteryTicket, Long> {

    Optional<LotteryTicket> findFirstByTenantIdAndStatusIn(Long tenantId, Collection<LotteryTicketStatus> statuses);

    Optional<LotteryTicket> findFirstByTenantIdAndStatusOrderByIdDesc(Long tenantId, LotteryTicketStatus status);

    List<LotteryTicket> findAllByStatus(LotteryTicketStatus status);

    /** Tickets in the draw: PENDING on a COMPLETED dossier, random order — the first rows win. */
    @Query(value = """
            SELECT e.*
            FROM lottery_ticket e
              JOIN tenant t ON t.id = e.tenant_id
            WHERE e.status = 'PENDING'
              AND t.status = 'COMPLETED'
            ORDER BY random()
            """, nativeQuery = true)
    List<LotteryTicket> findDrawTickets();

    /**
     * PENDING tickets whose dossier is not COMPLETED at draw time: the draw
     * cancels them (no cooldown).
     */
    @Query(value = """
            SELECT e.*
            FROM lottery_ticket e
              JOIN tenant t ON t.id = e.tenant_id
            WHERE e.status = 'PENDING'
              AND t.status <> 'COMPLETED'
            """, nativeQuery = true)
    List<LotteryTicket> findPendingOutOfDrawScope();

    List<LotteryTicket> findAllByStatusAndCooldownUntilLessThanEqualAndCooldownNotifiedAtIsNull(
            LotteryTicketStatus status, LocalDate maxCooldownUntil);
}
