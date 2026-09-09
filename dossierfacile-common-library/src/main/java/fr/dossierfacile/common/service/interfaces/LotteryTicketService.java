package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.lottery.LotteryStatusView;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Lottery ticket lifecycle. Invariant: in queue via opt-in ⟺ DRAWN ticket.
 * Creation is flag-gated; consumption/cancellation are unconditional (no-op without a ticket).
 */
public interface LotteryTicketService {

    String TENANT_LOTTERY_FEATURE_FLAG = "tenant_lottery";

    /** Days a non-drawn tenant must wait before applying again. */
    int COOLDOWN_DAYS = 3;

    /** The active (PENDING or DRAWN, see LotteryTicketStatus) ticket of this tenant, if any. */
    Optional<LotteryTicket> getActiveTicket(Long tenantId);

    /** End date of an ongoing cooldown; a new application is refused until then. */
    Optional<LocalDate> getCooldownEndDate(Long tenantId);

    /**
     * Registers a lottery application (PENDING). Idempotent on an active ticket; throws
     * IllegalStateException during a cooldown. Caller checks eligibility and flag.
     */
    LotteryTicket apply(Tenant tenant);

    /** Cancels the active ticket (withdrawal, partner link, regroup, type change). No-op without one. */
    void cancelActiveTicket(Tenant tenant);

    /** Cancels an already loaded ticket (CANCELLED + log): single cancellation rule, shared with the draw. */
    void cancelTicket(LotteryTicket ticket);

    /** Operator verdict: spends the draw win. No-op without a DRAWN ticket. */
    void consumeDrawnTicket(Long tenantId);

    /** Frontend lottery state; empty when the flag is off or nothing is ongoing. */
    Optional<LotteryStatusView> getPublicStatus(Long tenantId);
}
