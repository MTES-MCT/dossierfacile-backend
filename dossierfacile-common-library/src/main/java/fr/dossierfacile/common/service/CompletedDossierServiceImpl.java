package fr.dossierfacile.common.service;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.CompletedDossierService;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletedDossierServiceImpl implements CompletedDossierService {

    private final CompletedEligibilityService completedEligibilityService;
    private final TenantCommonRepository tenantCommonRepository;
    private final TenantLogCommonService tenantLogCommonService;
    private final TenantMapperForMail tenantMapperForMail;
    private final Optional<MailCommonService> mailCommonService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;

    @Override
    public TenantFileStatus toCompletedIfEligible(Tenant tenant, TenantFileStatus computedStatus) {
        if (computedStatus != TenantFileStatus.TO_PROCESS) {
            return computedStatus;
        }
        // A dossier already waiting in the operator queue stays there: the opt-in
        // only applies when entering the queue. Without this guard, a dossier that
        // became eligible after its submission (rollout increase) would silently
        // leave the queue at the first recomputation, possibly while an operator
        // is processing it
        if (tenant.getStatus() == TenantFileStatus.TO_PROCESS) {
            return computedStatus;
        }
        if (completedEligibilityService.canBeCompleted(tenant)) {
            return TenantFileStatus.COMPLETED;
        }
        return computedStatus;
    }

    @Override
    @Transactional
    public boolean switchToCompleted(Tenant tenant) {
        if (tenant.getStatus() != TenantFileStatus.TO_PROCESS || !completedEligibilityService.canBeCompleted(tenant)) {
            return false;
        }
        tenant.setStatus(TenantFileStatus.COMPLETED);
        tenantCommonRepository.save(tenant);
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        apartmentSharing.setLastUpdateDate(LocalDateTime.now(ZoneId.systemDefault()));
        apartmentSharingCommonService.save(apartmentSharing);
        return true;
    }

    // A COMPLETED dossier must never be exposed to partners: it goes back to the
    // operator queue, positioned at the time of the switch. The user's explicit
    // choice (validationRequested) is left untouched.
    @Override
    @Transactional
    public void switchBackToProcessing(Tenant tenant, UserApi userApi) {
        if (tenant.getStatus() != TenantFileStatus.COMPLETED) {
            return;
        }
        tenant.setStatus(TenantFileStatus.TO_PROCESS);
        tenant.setLastUpdateDate(LocalDateTime.now(ZoneId.systemDefault()));
        tenantCommonRepository.save(tenant);
        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.COMPLETED_SWITCHED_TO_PROCESS, tenant.getId()));
        // The full PDF was rendered with the COMPLETED design: it must not
        // survive the switch out of COMPLETED
        apartmentSharingCommonService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        if (userApi == null) {
            return;
        }
        String partnerName = StringUtils.isNotBlank(userApi.getName2()) ? userApi.getName2() : userApi.getName();
        mailCommonService.ifPresent(mailService -> {
            TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
            TransactionalUtil.afterCommit(() -> mailService.sendEmailCompletedSwitchedToProcessing(tenantDto, partnerName));
        });
    }
}
