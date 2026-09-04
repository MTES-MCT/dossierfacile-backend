package fr.dossierfacile.scheduler.tasks.lottery;

import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Daily verification lottery at 00:05 Europe/Paris (00:05 avoids the civil-day
 * boundary of the bypass window and DST edges). Idempotent: re-launchable from
 * the BO capacities screen. Also sends the end-of-cooldown notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryDrawTask extends AbstractTask {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final LotteryDrawService lotteryDrawService;

    @Scheduled(cron = "${lottery.draw.cron:0 5 0 * * *}", zone = "Europe/Paris")
    public void dailyLotteryDraw() {
        super.startTask(TaskName.TENANT_LOTTERY_DRAW);
        try {
            LocalDate today = LocalDate.now(PARIS);
            lotteryDrawService.executeDrawIfNeeded(today);
            lotteryDrawService.notifyCooldownEnded(today);
        } catch (Exception e) {
            log.error("Error during the daily lottery draw task", e);
        } finally {
            super.endTask();
        }
    }
}
