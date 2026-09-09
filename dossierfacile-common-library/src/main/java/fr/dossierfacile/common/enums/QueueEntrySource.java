package fr.dossierfacile.common.enums;

/**
 * Origin of a QUEUE_ENTERED log. Orthogonal to logDetails.bypass, which says
 * whether the entry counts against the daily lottery slots.
 * TODO(lottery-bypass): once every dossier goes through the lottery (all
 * application types, partners, rollout 100%), the bypass notion disappears —
 * drop PARTNER_LINK / COMPLETED_ROLLBACK and the bypass flag.
 */
public enum QueueEntrySource {
    /** api-tenant status recomputation (submission, legacy opt-in click). */
    SUBMISSION,
    /** BO status recomputation after operator edits. */
    BO_RECOMPUTE,
    /** Partner link switched a COMPLETED dossier back to processing. TODO(lottery-bypass): drop. */
    PARTNER_LINK,
    /** COMPLETED opt-in rollback action. TODO(lottery-bypass): drop. */
    COMPLETED_ROLLBACK,
    /** Lottery draw, or flush of pending tickets when the flag is deactivated. */
    LOTTERY_DRAW,
    /** Operator reprocessed a declined dossier. */
    BO_REPROCESS
}
