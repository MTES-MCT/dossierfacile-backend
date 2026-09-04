package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.LotteryPublicStatus;
import fr.dossierfacile.common.model.lottery.LotteryStatusView;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryTicketServiceImpl implements LotteryTicketService {

    private static final Set<LotteryTicketStatus> ACTIVE_STATUSES =
            Set.of(LotteryTicketStatus.PENDING, LotteryTicketStatus.DRAWN);

    private final LotteryTicketRepository lotteryTicketRepository;
    private final TenantLogCommonService tenantLogCommonService;
    private final FeatureFlagService featureFlagService;

    @Override
    public Optional<LotteryTicket> getActiveTicket(Long tenantId) {
        return lotteryTicketRepository.findFirstByTenantIdAndStatusIn(tenantId, ACTIVE_STATUSES);
    }

    @Override
    public Optional<LocalDate> getCooldownEndDate(Long tenantId) {
        return lotteryTicketRepository
                .findFirstByTenantIdAndStatusOrderByIdDesc(tenantId, LotteryTicketStatus.NOT_DRAWN)
                .map(LotteryTicket::getCooldownUntil)
                .filter(cooldownUntil -> cooldownUntil.isAfter(LocalDate.now()));
    }

    @Override
    @Transactional
    public LotteryTicket apply(Tenant tenant) {
        Optional<LotteryTicket> activeTicket = getActiveTicket(tenant.getId());
        if (activeTicket.isPresent()) {
            return activeTicket.get();
        }
        getCooldownEndDate(tenant.getId()).ifPresent(endDate -> {
            throw new IllegalStateException("A new lottery application is not allowed before " + endDate);
        });
        LotteryTicket ticket = LotteryTicket.builder()
                .tenantId(tenant.getId())
                .status(LotteryTicketStatus.PENDING)
                .build();
        return lotteryTicketRepository.save(ticket);
    }

    @Override
    @Transactional
    public void cancelActiveTicket(Tenant tenant) {
        getActiveTicket(tenant.getId()).ifPresent(this::cancelTicket);
    }

    @Override
    @Transactional
    public void consumeDrawnTicket(Long tenantId) {
        lotteryTicketRepository.findFirstByTenantIdAndStatusIn(tenantId, Set.of(LotteryTicketStatus.DRAWN))
                .ifPresent(ticket -> {
                    ticket.setStatus(LotteryTicketStatus.CONSUMED);
                    lotteryTicketRepository.save(ticket);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LotteryStatusView> getPublicStatus(Long tenantId) {
        if (!featureFlagService.isFeatureEnabled(TENANT_LOTTERY_FEATURE_FLAG)) {
            return Optional.empty();
        }
        Optional<LotteryTicket> activeTicket = getActiveTicket(tenantId);
        if (activeTicket.isPresent()) {
            LotteryPublicStatus status = activeTicket.get().getStatus() == LotteryTicketStatus.PENDING
                    ? LotteryPublicStatus.PENDING
                    : LotteryPublicStatus.DRAWN;
            return Optional.of(new LotteryStatusView(status, null));
        }
        return getCooldownEndDate(tenantId)
                .map(endDate -> new LotteryStatusView(LotteryPublicStatus.COOLDOWN, endDate));
    }

    @Override
    @Transactional
    public void cancelTicket(LotteryTicket ticket) {
        ticket.setStatus(LotteryTicketStatus.CANCELLED);
        lotteryTicketRepository.save(ticket);
        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.LOTTERY_TICKET_CANCELLED, ticket.getTenantId()));
    }
}
