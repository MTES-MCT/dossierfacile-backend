package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.LotteryDraw;

import java.time.LocalDate;
import java.util.Optional;

/** Daily lottery execution, shared by the task-scheduler cron and the BO actions. */
public interface LotteryDrawService {

    /**
     * Runs the draw for the date, idempotent (no-op if a draw exists, unless
     * allow-multiple-per-day). Available slots = daily_count(drawDate) - bypass
     * No capacity row: no draw recorded,
     * re-launchable the same day. Every executed draw settles every application
     * (DRAWN / NOT_DRAWN / CANCELLED if the dossier is not COMPLETED — no cooldown).
     * No slot: draw recorded, tickets stay PENDING. One transaction per ticket.
     */
    Optional<LotteryDraw> executeDrawIfNeeded(LocalDate drawDate);

    /** True when lottery.draw.allow-multiple-per-day is enabled (preprod). */
    boolean allowsMultipleDrawsPerDay();

    /** Notifies (once) the NOT_DRAWN tenants whose cooldown ended. No-op when the flag is off. */
    int notifyCooldownEnded(LocalDate today);

    /**
     * On flag deactivation: PENDING applications on COMPLETED dossiers are granted
     * (DRAWN + TO_PROCESS + mail), the others cancelled. Returns the flushed count.
     */
    int flushPendingTicketsToProcessing();

    /**
     * On flag activation: opt-ins already waiting in the queue keep their place —
     * they get a DRAWN ticket outside any draw (idempotent). Returns the granted count.
     */
    int grantTicketsToQueuedOptIns();
}
