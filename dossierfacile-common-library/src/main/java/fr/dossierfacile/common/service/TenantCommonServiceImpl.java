package fr.dossierfacile.common.service;

import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.ApartmentSharingMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class TenantCommonServiceImpl implements TenantCommonService {

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final DocumentCommonRepository documentRepository;
    private final TenantCommonRepository tenantCommonRepository;
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final TenantMapperForMail tenantMapperForMail;
    private final ApartmentSharingMapperForMail apartmentSharingMapperForMail;
    private ApartmentSharingCommonService apartmentSharingCommonService;

    @Override
    public void deleteTenantData(Tenant tenant) {

        Optional<ApartmentSharing> apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (apartmentSharing.isEmpty()) {
            return;
        }

        if (apartmentSharing.get().getPdfDossierFile() != null) {
            apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing.get());
        }

        documentRepository.deleteAll(tenant.getDocuments());
        tenant.setDocuments(new ArrayList<>());

        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> {documentRepository.deleteAll(guarantor.getDocuments());
                guarantor.setDocuments(new ArrayList<>());});
    }

    @Override
    public Tenant findByKeycloakId(String keycloakId) {
        return tenantCommonRepository.findByKeycloakId(keycloakId);
    }

    @Override
    public Long getTenantRank(Long id) {
        return tenantCommonRepository.getTenantRank(id);
    }

    @Override
    public void changeTenantStatusToValidated(Tenant tenant) {
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenantCommonRepository.save(tenant);

        // TODO: Remove after sharing page is implemented
        boolean hasLinks = tenant.getApartmentSharing().getApartmentSharingLinks().stream()
            .anyMatch(link -> link.getLinkType() == ApartmentSharingLinkType.LINK);
        if (!hasLinks) {
            ApartmentSharingLink link = buildApartmentSharingLink(tenant.getApartmentSharing(), tenant.getId(), false);
            ApartmentSharingLink linkFull = buildApartmentSharingLink(tenant.getApartmentSharing(), tenant.getId(), true);
            apartmentSharingLinkRepository.save(link);
            apartmentSharingLinkRepository.save(linkFull);
        }

        // prepare for mail
        ApartmentSharingDto apartmentSharingDto = apartmentSharingMapperForMail.toDto(tenant.getApartmentSharing());

        // sendCallBack is sent after Commit
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);

        TransactionalUtil.afterCommit(() -> {
            try {
                log.info("Tenant [{}] validated. All tenants validated: {}",
                    tenant.getId(),
                    apartmentSharingDto.getTenants().stream().allMatch(t -> t.getStatus() == TenantFileStatus.VALIDATED));
                // Note: Email notifications are module-specific and handled by the calling module
            } catch (Exception e) {
                log.error("CAUTION Unable to log tenant validation", e);
            }
        });
    }

    private ApartmentSharingLink buildApartmentSharingLink(ApartmentSharing apartmentSharing, Long userId, boolean fullData) {
        return ApartmentSharingLink.builder()
                .apartmentSharing(apartmentSharing)
                .token(UUID.randomUUID())
                .creationDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMonths(1))
                .fullData(fullData)
                .linkType(ApartmentSharingLinkType.LINK)
                .title("Lien créé le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .createdBy(userId)
                .build();
    }
}
