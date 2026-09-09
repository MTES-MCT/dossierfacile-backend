package fr.dossierfacile.common.enums;

/**
 * Lottery ticket lifecycle. Active = PENDING or DRAWN, at most one per tenant 
 * The three other statuses are inactive tickets.
 */
public enum LotteryTicketStatus {
    /** Waiting for the next draw. */
    PENDING,
    /** Drawn: in the operator queue, until the operator verdict. */
    DRAWN,
    /** Lost a draw: cooldown before applying again. */
    NOT_DRAWN,
    /** Withdrawn or voided (dossier not COMPLETED at draw, partner link, type change, regroup). */
    CANCELLED,
    /** Operator verdict rendered: the draw win is spent. */
    CONSUMED
}
