package fr.dossierfacile.common.service;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.LotteryDraw;
import fr.dossierfacile.common.entity.ProcessingCapacity;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.LotteryDrawRepository;
import fr.dossierfacile.common.repository.ProcessingCapacityRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.service.interfaces.LotteryTicketService.COOLDOWN_DAYS;
import static fr.dossierfacile.common.service.interfaces.LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryDrawServiceImpl implements LotteryDrawService {

    // Self-reference to go through the Spring proxy for @Transactional(REQUIRES_NEW)
    @Lazy
    @Autowired
    private LotteryDrawServiceImpl self;

    private final FeatureFlagService featureFlagService;
    private final ProcessingCapacityRepository processingCapacityRepository;
    private final LotteryDrawRepository lotteryDrawRepository;
    private final LotteryTicketRepository lotteryTicketRepository;
    private final LotteryTicketService lotteryTicketService;
    private final TenantLogRepository tenantLogRepository;
    private final TenantCommonRepository tenantCommonRepository;
    private final TenantLogCommonService tenantLogCommonService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final TenantMapperForMail tenantMapperForMail;
    private final Optional<MailCommonService> mailCommonService;

    // Preprod only: allows several draws on the same day
    @Value("${lottery.draw.allow-multiple-per-day:false}")
    private boolean allowMultipleDrawsPerDay;

    @Override
    public boolean allowsMultipleDrawsPerDay() {
        return allowMultipleDrawsPerDay;
    }

    @Override
    public Optional<LotteryDraw> executeDrawIfNeeded(LocalDate drawDate) {
        if (!featureFlagService.isFeatureEnabled(TENANT_LOTTERY_FEATURE_FLAG)) {
            log.info("Lottery draw skipped: feature flag {} is off", TENANT_LOTTERY_FEATURE_FLAG);
            return Optional.empty();
        }
        Optional<LotteryDraw> existingDraw = lotteryDrawRepository.findFirstByDrawDateOrderByIdDesc(drawDate);
        if (existingDraw.isPresent() && !allowMultipleDrawsPerDay) {
            log.info("Lottery draw already executed for {}: draw {}", drawDate, existingDraw.get().getId());
            return existingDraw;
        }
        ProcessingCapacity capacity = processingCapacityRepository.findByDate(drawDate);
        if (capacity == null) {
            log.warn("Lottery draw aborted: no processing capacity defined for {} — no draw recorded, " +
                    "the draw can be re-launched once the capacity is filled in", drawDate);
            return Optional.empty();
        }
        // Previous civil day: deterministic, a re-launch computes the same slot count
        // TODO(lottery-bypass): availableSlots = dailyCount once every dossier goes
        // through the lottery (BO_REPROCESS remains, negligible and absorbed)
        long bypassCount = tenantLogRepository.countBypassQueueEntries(
                drawDate.minusDays(1).atStartOfDay(), drawDate.atStartOfDay());
        int availableSlots = capacity.getDailyCount() - (int) bypassCount;
        List<LotteryTicket> tickets = lotteryTicketRepository.findDrawTickets();

        LotteryDraw draw = self.createDraw(drawDate, capacity.getDailyCount(), (int) bypassCount, availableSlots, tickets.size());
        // Dossier not COMPLETED at draw time => application cancelled (no cooldown)
        cancelOutOfScopeTickets(drawDate);
        if (availableSlots <= 0) {
            log.warn("Lottery draw for {}: no available slot ({} = capacity {} - bypass {}), no ticket drawn " +
                            "({} tickets stay for the next draw)",
                    drawDate, availableSlots, capacity.getDailyCount(), bypassCount, tickets.size());
            return Optional.of(draw);
        }

        // The random ordering IS the draw: the first availableSlots tickets win
        int drawnCount = 0;
        for (int i = 0; i < tickets.size(); i++) {
            LotteryTicket ticket = tickets.get(i);
            try {
                if (i < availableSlots) {
                    if (self.drawTicket(ticket.getId(), draw.getId())) {
                        drawnCount++;
                    }
                } else {
                    self.markNotDrawn(ticket.getId(), draw.getId(), drawDate.plusDays(COOLDOWN_DAYS));
                }
            } catch (Exception e) {
                log.error("Lottery draw for {}: failed to process ticket {} (tenant {})",
                        drawDate, ticket.getId(), ticket.getTenantId(), e);
            }
        }
        draw = self.updateDrawnCount(draw.getId(), drawnCount);
        if (drawnCount > 0) {
            // Immediate refresh: drawn dossiers must not wait the BO's 5-minute cycle
            tenantCommonRepository.refreshRank();
        }
        log.info("Lottery draw for {}: capacity {}, bypass {}, slots {}, tickets {}, drawn {}",
                drawDate, capacity.getDailyCount(), bypassCount, availableSlots, tickets.size(), drawnCount);
        return Optional.of(draw);
    }

    @Override
    public int notifyCooldownEnded(LocalDate today) {
        if (!featureFlagService.isFeatureEnabled(TENANT_LOTTERY_FEATURE_FLAG)) {
            return 0;
        }
        List<LotteryTicket> toNotify = lotteryTicketRepository
                .findAllByStatusAndCooldownUntilLessThanEqualAndCooldownNotifiedAtIsNull(LotteryTicketStatus.NOT_DRAWN, today);
        int notified = 0;
        for (LotteryTicket ticket : toNotify) {
            try {
                if (self.notifyTicket(ticket.getId())) {
                    notified++;
                }
            } catch (Exception e) {
                log.error("Lottery cooldown notification failed for ticket {} (tenant {})",
                        ticket.getId(), ticket.getTenantId(), e);
            }
        }
        if (!toNotify.isEmpty()) {
            log.info("Lottery cooldown notifications: {} sent out of {} ended cooldowns", notified, toNotify.size());
        }
        return notified;
    }

    @Override
    public int flushPendingTicketsToProcessing() {
        List<LotteryTicket> pendingTickets = lotteryTicketRepository.findAllByStatus(LotteryTicketStatus.PENDING);
        int flushed = 0;
        for (LotteryTicket ticket : pendingTickets) {
            try {
                // A flush is a draw where everyone wins, without a draw record
                if (self.drawTicket(ticket.getId(), null)) {
                    flushed++;
                }
            } catch (Exception e) {
                log.error("Lottery flush failed for ticket {} (tenant {})", ticket.getId(), ticket.getTenantId(), e);
            }
        }
        if (flushed > 0) {
            tenantCommonRepository.refreshRank();
        }
        log.info("Lottery flush: {} pending applications switched to TO_PROCESS out of {}", flushed, pendingTickets.size());
        return flushed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected LotteryDraw createDraw(LocalDate drawDate, int dailyCount, int bypassCount, int availableSlots, int ticketCount) {
        return lotteryDrawRepository.save(LotteryDraw.builder()
                .drawDate(drawDate)
                .dailyCount(dailyCount)
                .bypassCount(bypassCount)
                .availableSlots(availableSlots)
                .ticketCount(ticketCount)
                .drawnCount(0)
                .build());
    }

    private void cancelOutOfScopeTickets(LocalDate drawDate) {
        List<LotteryTicket> outOfScope = lotteryTicketRepository.findPendingOutOfDrawScope();
        int cancelled = 0;
        for (LotteryTicket ticket : outOfScope) {
            try {
                if (self.cancelTicketOutOfScope(ticket.getId())) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.error("Lottery draw for {}: failed to cancel out-of-scope ticket {} (tenant {})",
                        drawDate, ticket.getId(), ticket.getTenantId(), e);
            }
        }
        if (cancelled > 0) {
            log.info("Lottery draw for {}: {} applications cancelled (dossier not COMPLETED at draw time)",
                    drawDate, cancelled);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean cancelTicketOutOfScope(Long ticketId) {
        LotteryTicket ticket = lotteryTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getStatus() != LotteryTicketStatus.PENDING) {
            return false;
        }
        Tenant tenant = tenantCommonRepository.findById(ticket.getTenantId()).orElse(null);
        if (tenant != null && tenant.getStatus() == TenantFileStatus.COMPLETED) {
            // Back in shape since the query: leave the application alone
            return false;
        }
        lotteryTicketService.cancelTicket(ticket);
        return true;
    }

    /**
     * DRAWN + dossier to TO_PROCESS. A dossier that left COMPLETED since
     * the tickets were listed is cancelled (the slot is lost — rare, accepted).
     * drawId is null for a flush (grant outside any draw).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean drawTicket(Long ticketId, Long drawId) {
        LotteryTicket ticket = lotteryTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getStatus() != LotteryTicketStatus.PENDING) {
            return false;
        }
        Tenant tenant = tenantCommonRepository.findById(ticket.getTenantId()).orElse(null);
        if (tenant == null) {
            return false;
        }
        if (tenant.getStatus() != TenantFileStatus.COMPLETED) {
            lotteryTicketService.cancelTicket(ticket);
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        ticket.setStatus(LotteryTicketStatus.DRAWN);
        ticket.setDrawnAt(now);
        ticket.setLotteryDrawId(drawId);
        lotteryTicketRepository.save(ticket);

        tenant.setStatus(TenantFileStatus.TO_PROCESS);
        tenant.setLastUpdateDate(now);
        tenantCommonRepository.save(tenant);
        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.LOTTERY_DRAWN, tenant.getId()));
        // Ticket already DRAWN: logged with bypass=false
        tenantLogCommonService.logQueueEntered(tenant.getId(), QueueEntrySource.LOTTERY_DRAW);
        // The full PDF was rendered with the COMPLETED design: it must not
        // survive the switch out of COMPLETED
        apartmentSharingCommonService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markNotDrawn(Long ticketId, Long drawId, LocalDate cooldownUntil) {
        LotteryTicket ticket = lotteryTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getStatus() != LotteryTicketStatus.PENDING) {
            return;
        }
        Tenant tenant = tenantCommonRepository.findById(ticket.getTenantId()).orElse(null);
        if (tenant == null || tenant.getStatus() != TenantFileStatus.COMPLETED) {
            // Left COMPLETED since the tickets were listed: cancelled, not penalized
            lotteryTicketService.cancelTicket(ticket);
            return;
        }
        ticket.setStatus(LotteryTicketStatus.NOT_DRAWN);
        ticket.setLotteryDrawId(drawId);
        ticket.setCooldownUntil(cooldownUntil);
        lotteryTicketRepository.save(ticket);
        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.LOTTERY_NOT_DRAWN, ticket.getTenantId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected LotteryDraw updateDrawnCount(Long drawId, int drawnCount) {
        LotteryDraw draw = lotteryDrawRepository.findById(drawId).orElseThrow();
        draw.setDrawnCount(drawnCount);
        return lotteryDrawRepository.save(draw);
    }

    // Marked notified even when no mail can be sent: never retried
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean notifyTicket(Long ticketId) {
        LotteryTicket ticket = lotteryTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getCooldownNotifiedAt() != null) {
            return false;
        }
        ticket.setCooldownNotifiedAt(LocalDateTime.now(ZoneId.systemDefault()));
        lotteryTicketRepository.save(ticket);
        Tenant tenant = tenantCommonRepository.findById(ticket.getTenantId()).orElse(null);
        if (tenant == null || tenant.getStatus() != TenantFileStatus.COMPLETED) {
            // "Apply again" would be misleading on a dossier that left COMPLETED
            return false;
        }
        if (mailCommonService.isEmpty()) {
            return false;
        }
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        MailCommonService mailService = mailCommonService.get();
        TransactionalUtil.afterCommit(() -> mailService.sendEmailLotteryCooldownEnded(tenantDto));
        return true;
    }
}
