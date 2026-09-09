package fr.dossierfacile.common.model.lottery;

import fr.dossierfacile.common.enums.LotteryPublicStatus;

import java.time.LocalDate;

/** Lottery state for the tenant frontend; {@code nextEligibleDate} only set with COOLDOWN. */
public record LotteryStatusView(LotteryPublicStatus status, LocalDate nextEligibleDate) {
}
