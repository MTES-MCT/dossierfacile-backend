package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.repository.UserApiRepository;
import fr.dossierfacile.common.service.interfaces.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.dossierfacile.common.constants.PartnerConstants.DF_OWNER_NAME;
import static java.time.temporal.ChronoUnit.DAYS;
import static fr.dossierfacile.common.enums.ApartmentSharingLinkType.OWNER;
import static fr.dossierfacile.common.enums.ApartmentSharingLinkType.PARTNER;
import static fr.dossierfacile.common.enums.LinkType.*;

@Service
@Slf4j
@AllArgsConstructor
public class ApartmentSharingLinkService {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final LinkLogService linkLogService;
    private final TenantCommonRepository tenantCommonRepository;
    private final UserApiRepository userApiRepository;
    private final TenantUserApiRepository tenantUserApiRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final KeycloakCommonService keycloakCommonService;
    private final MailCommonService mailCommonService;
    private final LogService logService;

    /**
     * Returns filtered links according to business rules:
     * - Excludes DF_OWNER partner links
     * - Excludes non-full-data partner links
     * - Includes all non-partner links
     */
    public List<ApartmentSharingLink> getFilteredLinks(ApartmentSharing apartmentSharing) {
        var links = apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing);

        // Get the ID of the DF_OWNER partner to exclude it
        Long dfOwnerPartnerId = userApiRepository.findByName(DF_OWNER_NAME)
                .map(UserApi::getId)
                .orElse(null);

        List<ApartmentSharingLink> noPartnerLinks = links.stream()
                .filter(l -> l.getPartnerId() == null)
                .toList();

        // Filter out links with the DF_OWNER partner and non-full-data partner links
        List<ApartmentSharingLink> fullDataPerPartnerLinks = links.stream()
                .filter(l -> l.getPartnerId() != null && l.isFullData())
                .filter(l -> !l.getPartnerId().equals(dfOwnerPartnerId))
                .toList();

        List<ApartmentSharingLink> validLinks = new ArrayList<>();
        validLinks.addAll(noPartnerLinks);
        validLinks.addAll(fullDataPerPartnerLinks);
        return validLinks;
    }

    public List<ApartmentSharingLinkModel> getLinks(ApartmentSharing apartmentSharing) {
        return getFilteredLinks(apartmentSharing).stream()
                .map(link -> mapApartmentSharingLink(link, apartmentSharing))
                .toList();
    }

    public ApartmentSharingLinkModel getDefaultLink(ApartmentSharing apartmentSharing, Tenant tenant, boolean isFullData) {
        List<ApartmentSharingLink> validLinks = apartmentSharingLinkRepository
                .findValidDefaultLinks(apartmentSharing.getId(), isFullData, tenant.getId());

        Optional<ApartmentSharingLink> linkWithFurthestExpiration = validLinks.stream().findFirst();

        // if no valid link exists -> create new link
        if (linkWithFurthestExpiration.isEmpty()) {
            return createNewLink(apartmentSharing, isFullData, tenant.getId());
        }

        ApartmentSharingLink link = linkWithFurthestExpiration.get();

        LocalDateTime now = LocalDateTime.now();
        long daysUntilExpiration = DAYS.between(now, link.getExpirationDate());

        // If valid link exist and expiration date is in more than 10 days -> return this link
        if (daysUntilExpiration > 10) {
            return mapApartmentSharingLink(link, apartmentSharing);
        }

        // if expiration date is in less than 10 days -> create new link
        return createNewLink(apartmentSharing, isFullData, tenant.getId());
    }

    private ApartmentSharingLinkModel createNewLink(ApartmentSharing apartmentSharing, boolean fullData, Long tenantId) {
        ApartmentSharingLink newLink = ApartmentSharingLink.builder()
                .apartmentSharing(apartmentSharing)
                .token(UUID.randomUUID())
                .creationDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMonths(1))
                .fullData(fullData)
                .linkType(ApartmentSharingLinkType.LINK)
                .title("Lien créé le " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .createdBy(tenantId)
                .build();

        apartmentSharingLinkRepository.save(newLink);
        log.info("Created new default link for apartmentSharing {} with fullData={}", apartmentSharing.getId(), fullData);

        return mapApartmentSharingLink(newLink, apartmentSharing);
    }


    private ApartmentSharingLinkModel mapApartmentSharingLink(ApartmentSharingLink link, ApartmentSharing apartmentSharing) {
        LinkLogServiceImpl.FirstAndLastVisit firstAndLastVisit = linkLogService.getFirstAndLastVisit(link.getToken(), apartmentSharing);
        long nbVisits = linkLogService.countVisits(link.getToken(), apartmentSharing);

        String createdByFullName = null;
        if (link.getCreatedBy() != null) {
            Tenant creator = tenantCommonRepository.findById(link.getCreatedBy()).orElse(null);
            if (creator != null) {
                createdByFullName = creator.getFullName();
            }
        }

        String url = (link.isFullData() ? "/file/" : "/public-file/") + link.getToken();

        return ApartmentSharingLinkModel.builder()
                .id(link.getId())
                .creationDate(link.getCreationDate())
                .ownerEmail(link.getEmail())
                .lastVisit(firstAndLastVisit.last().orElse(null))
                .firstVisit(firstAndLastVisit.first().orElse(null))
                .enabled(!link.isDisabled())
                .deleted(link.isDeleted())
                .fullData(link.isFullData())
                .expirationDate(link.getExpirationDate())
                .title(link.getTitle())
                .type(link.getLinkType().toString())
                .nbVisits(nbVisits)
                .createdBy(createdByFullName)
                .url(url)
                .build();
    }

    public void updateStatus(Long linkId, boolean enabled, ApartmentSharing apartmentSharing) {
        var link = apartmentSharingLinkRepository.findByIdAndApartmentSharingAndDeletedIsFalse(linkId, apartmentSharing).orElseThrow(NotFoundException::new);
        link.setDisabled(!enabled);
        linkLogService.createNewLog(link, enabled ? ENABLED_LINK : DISABLED_LINK);
        apartmentSharingLinkRepository.save(link);
    }

    public void delete(Long linkId) {
        var link = apartmentSharingLinkRepository.findById(linkId).orElseThrow(NotFoundException::new);

        if (link.getLinkType() == OWNER) {
            // Special logic for OWNER links: delete OWNER + associated DF_OWNER PARTNER links
            deleteOwnerLink(link);
        } else if (link.getLinkType() == PARTNER && link.getPartnerId() != null) {
            // Special logic for PARTNER links: revoke OAuth access + delete link pair
            deletePartnerLinkWithRevocation(link);
        } else {
            // Standard behavior for LINK and MAIL sharing links
            deleteLinkStandard(link);
        }
    }

    private void deleteOwnerLink(ApartmentSharingLink ownerLink) {
        ApartmentSharing apartmentSharing = ownerLink.getApartmentSharing();

        // Delete OWNER link
        deleteLinkStandard(ownerLink);

        // Delete related PARTNER links
        userApiRepository.findByName(DF_OWNER_NAME).map(UserApi::getId).ifPresent(
                dfOwnerPartnerId -> apartmentSharing.getApartmentSharingLinks().stream()
                        .filter(l -> l.getLinkType() == PARTNER
                                && dfOwnerPartnerId.equals(l.getPartnerId())
                                && !l.isDeleted())
                        .forEach(this::deleteLinkStandard));

    }

    private void deletePartnerLinkWithRevocation(ApartmentSharingLink link) {
        ApartmentSharing apartmentSharing = link.getApartmentSharing();

        // Revoke access for all tenants of the ApartmentSharing
        tenantUserApiRepository.findAllByApartmentSharingAndUserApi(
            apartmentSharing.getId(),
            link.getPartnerId()
        ).forEach(this::revokeAccess);

        // Remove sharing link pair (fullData and non fullData)
        apartmentSharing.getApartmentSharingLinks().stream()
            .filter(l -> l.getLinkType() == PARTNER
                      && link.getPartnerId().equals(l.getPartnerId())
                      && !l.isDeleted())
            .forEach(this::deleteLinkStandard);
    }

    private void deleteLinkStandard(ApartmentSharingLink link) {
        log.info("Delete token: " + link.getToken() + " by " + link.getLinkType()
                + " on apartmentSharing" + link.getApartmentSharing().getId());
        linkLogService.createNewLog(link, DELETED_LINK_TOKEN);
        link.setExpirationDate(LocalDateTime.now());
        link.setDeleted(true);
        apartmentSharingLinkRepository.save(link);
    }

    public void delete(Long linkId, Tenant tenant) {
        boolean hasAccess = tenant.getApartmentSharing().getApartmentSharingLinks().stream()
                .anyMatch(link -> link.getId().equals(linkId));
        if (hasAccess) {
            delete(linkId);
        }
    }

    private void revokeAccess(TenantUserApi tenantUserApi) {
        UserApi userApi = tenantUserApi.getUserApi();
        Tenant tenant = tenantUserApi.getTenant();
        tenant.setLastUpdateDate(LocalDateTime.now());
        tenantCommonRepository.save(tenant);

        tenantUserApiRepository.delete(tenantUserApi);
        partnerCallBackService.sendRevokedAccessCallback(tenant, userApi);
        keycloakCommonService.revokeUserConsent(tenant, userApi);
        logService.savePartnerAccessRevocationLog(tenant, userApi);
        mailCommonService.sendEmailPartnerAccessRevoked(tenant, userApi, tenant);

        log.info("Revoked access of partner {} to tenant {}", userApi.getId(), tenant.getId());
    }

    private boolean isValidLink(ApartmentSharingLink link) {
        return !link.isDeleted() && (link.getExpirationDate() == null || LocalDateTime.now().isBefore(link.getExpirationDate()));
    }

    public void deleteLinks(List<Long> linkIds, Tenant tenant) {
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (linkIds.contains(link.getId())) {
                delete(link.getId());
            }
        }
    }

    public void disableValidLinks(Tenant tenant) {
        log.info("Pause all valid links on apartmentSharing" + tenant.getApartmentSharing().getId());
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (isValidLink(link) && !link.isDisabled()) {
                link.setDisabled(true);
                apartmentSharingLinkRepository.save(link);
            }
        }
    }

    public void enableValidLinks(Tenant tenant) {
        log.info("Enable all valid links on apartmentSharing" + tenant.getApartmentSharing().getId());
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (isValidLink(link) && link.isDisabled()) {
                link.setDisabled(false);
                apartmentSharingLinkRepository.save(link);
            }
        }
    }

    public void regenerateToken(UUID token) {
        var link = apartmentSharingLinkRepository.findByToken(token).orElseThrow(NotFoundException::new);
        linkLogService.save(LinkLog.builder().creationDate(LocalDateTime.now()).linkType(REBUILT_TOKENS).token(token).apartmentSharing(link.getApartmentSharing()).build());
        UUID newToken = UUID.randomUUID();
        log.info("Regenerate token " + token + " to " + newToken);
        link.setToken(newToken);
        apartmentSharingLinkRepository.save(link);
    }

    public void updateExpirationDate(Long linkId, LocalDateTime expirationDate, ApartmentSharing apartmentSharing) {
        var link = apartmentSharingLinkRepository.findByIdAndApartmentSharingAndDeletedIsFalse(linkId, apartmentSharing)
                .orElseThrow(NotFoundException::new);
        log.info("Update expiration date for link: " + link.getId() + " from " + link.getExpirationDate() + " to " + expirationDate);
        link.setExpirationDate(expirationDate);
        apartmentSharingLinkRepository.save(link);
    }

    public void updateTitle(Long linkId, String title, ApartmentSharing apartmentSharing) {
        var link = apartmentSharingLinkRepository.findByIdAndApartmentSharingAndDeletedIsFalse(linkId, apartmentSharing)
                .orElseThrow(NotFoundException::new);
        log.info("Update title for link: " + link.getId() + " from " + link.getTitle() + " to " + title);
        link.setTitle(title);
        apartmentSharingLinkRepository.save(link);
    }


}
