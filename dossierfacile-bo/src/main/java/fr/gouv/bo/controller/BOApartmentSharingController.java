package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRuleLevel;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.gouv.bo.dto.ApartmentSharingLinkEnrichedDTO;
import fr.gouv.bo.dto.DisplayableFile;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.PartnerDTO;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/bo/colocation")
@Slf4j
public class BOApartmentSharingController {

    private static final String REDIRECT_BO_COLOCATION = "redirect:/bo/colocation/";

    private static final String LOG_SER = "logser";
    private static final String TENANTS = "tenants";
    private static final String TENANT_SERVICE = "tenantService";
    private static final String DOCUMENT_RULE_LEVEL = "documentRuleLevel";
    private static final String MESSAGE_DTO = "messageDTO";
    private static final String PARTNERS_LIST = "partnersList";
    private static final String PARTNER_UPDATE = "partnerUpdate";
    private static final String TENANT_PRINCIPAL = "tenantPrincipal";
    private static final String APARTMENT_SHARING = "apartmentSharing";
    private static final String PARTNER_LIST_BY_TENANT = "partnerListByTenant";
    private static final String NOW = "now";
    private static final String FILES_BY_DOCUMENT = "filesByDocument";
    private static final String TENANT_BASE_URL = "tenantBaseUrl";
    private static final String ACTIVE_LINKS = "activeLinks";
    private static final String DELETED_LINKS = "deletedLinks";

    private final TenantService tenantService;
    private final ApartmentSharingLinkService apartmentSharingLinkService;
    private final UserApiService userApiService;
    private final TenantLogService logService;
    private final LinkLogService linkLogService;
    private final LinkLogRepository linkLogRepository;
    private final UserService userService;

    @Value("${tenant.base.url}")
    private String tenantBaseUrl;

    @GetMapping("/{id}")
    public String view(Model model, @PathVariable("id") Long id) {

        MessageDTO messageDTO = new MessageDTO();
        PartnerDTO partnerDTO = new PartnerDTO();

        List<Tenant> tenants = tenantService.findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(id);

        // Enrich apartment sharing links with visit data and creator info
        List<ApartmentSharingLink> filteredLinks = apartmentSharingLinkService.getFilteredLinks(tenants.getFirst().getApartmentSharing());

        List<ApartmentSharingLinkEnrichedDTO> enrichedLinks = enrichApartmentSharingLinks(filteredLinks);

        List<ApartmentSharingLinkEnrichedDTO> activeLinks = enrichedLinks.stream()
                .filter(link -> !link.isDeleted() && link.getExpirationDate().isAfter(LocalDateTime.now()))
                .toList();

        List<ApartmentSharingLinkEnrichedDTO> deletedLinks = enrichedLinks.stream()
                .filter(link -> !activeLinks.contains(link))
                .toList();

        model.addAttribute(TENANTS, tenants);
        model.addAttribute(LOG_SER, logService);
        model.addAttribute(MESSAGE_DTO, messageDTO);
        model.addAttribute(PARTNER_UPDATE, partnerDTO);
        model.addAttribute(TENANT_PRINCIPAL, tenants.getFirst());
        model.addAttribute(TENANT_SERVICE, tenantService);
        model.addAttribute(DOCUMENT_RULE_LEVEL, DocumentRuleLevel.WARN);
        model.addAttribute(PARTNER_LIST_BY_TENANT, userApiService);
        model.addAttribute(PARTNERS_LIST, userApiService.getAllPartners());
        model.addAttribute(APARTMENT_SHARING, tenants.getFirst().getApartmentSharing());
        model.addAttribute(NOW, LocalDateTime.now());
        model.addAttribute(FILES_BY_DOCUMENT, getFilesByDocument(tenants));
        model.addAttribute(TENANT_BASE_URL, tenantBaseUrl);
        model.addAttribute(DELETED_LINKS, deletedLinks);
        model.addAttribute(ACTIVE_LINKS, activeLinks);

        return "bo/apartment-sharing-view";
    }

    @PostMapping("/{id}/tokens/{token}")
    public String regenerateToken(Model model, @PathVariable UUID token, @PathVariable Long id) {
        apartmentSharingLinkService.regenerateToken(token);
        return REDIRECT_BO_COLOCATION + id;
    }


    @DeleteMapping("/{id}/apartmentSharingLinks/{link_id}")
    public String deleteToken(Model model, @PathVariable Long id, @PathVariable("link_id") Long linkId) {
        apartmentSharingLinkService.delete(linkId);
        return REDIRECT_BO_COLOCATION + id;
    }

    @PutMapping("/{id}/apartmentSharingLinks/{link_id}")
    public String updateTokenStatus(Model model, @PathVariable("link_id") Long linkId, @PathVariable Long id, @RequestParam boolean enabled) {
        List<Tenant> tenants = tenantService.findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(id);
        apartmentSharingLinkService.updateStatus(linkId, enabled, tenants.getFirst().getApartmentSharing());
        return REDIRECT_BO_COLOCATION + id;
    }

    private Map<Long, List<DisplayableFile>> getFilesByDocument(List<Tenant> tenants) {
        return tenants.stream()
                .flatMap(BOApartmentSharingController::getAllDocuments)
                .collect(Collectors.toMap(Document::getId, DisplayableFile::allOf));
    }

    private static Stream<Document> getAllDocuments(Tenant tenant) {
        Stream<Document> tenantDocuments = tenant.getDocuments().stream();
        Stream<Document> guarantorDocuments = tenant.getGuarantors().stream()
                .flatMap(guarantor -> guarantor.getDocuments().stream());
        return Stream.concat(tenantDocuments, guarantorDocuments);
    }
    
    private List<ApartmentSharingLinkEnrichedDTO> enrichApartmentSharingLinks(List<ApartmentSharingLink> links) {
        if (links.isEmpty()) {
            return new ArrayList<>();
        }

        List<ApartmentSharingLinkEnrichedDTO> enrichedLinks = new ArrayList<>();

        ApartmentSharing apartmentSharing = links.getFirst().getApartmentSharing();
        List<LinkLog> allLinkLogs = linkLogRepository.findByApartmentSharing(apartmentSharing);

        Map<UUID, List<LinkLog>> logsByToken = allLinkLogs.stream()
                .collect(Collectors.groupingBy(LinkLog::getToken));

        // Define visit log types
        List<LinkType> visitLogTypes = List.of(LinkType.FULL_APPLICATION, LinkType.LIGHT_APPLICATION, LinkType.DOCUMENT);

        for (ApartmentSharingLink link : links) {
            ApartmentSharingLinkEnrichedDTO dto = ApartmentSharingLinkEnrichedDTO.fromEntity(link);

            List<LinkLog> logsForToken = logsByToken.getOrDefault(link.getToken(), List.of());

            List<LinkLog> accessLogs = logsForToken.stream()
                    .filter(log -> visitLogTypes.contains(log.getLinkType()))
                    .sorted(Comparator.comparing(LinkLog::getCreationDate).reversed())
                    .toList();
            dto.setAccessLogs(accessLogs);

            if (!accessLogs.isEmpty()) {
                dto.setFirstVisit(accessLogs.getLast().getCreationDate());
                dto.setLastVisit(accessLogs.getFirst().getCreationDate());
                dto.setNbVisits(accessLogs.size());
            }

            long nbDownloads = logsForToken.stream()
                    .filter(log -> log.getLinkType() == LinkType.DOCUMENT)
                    .count();
            dto.setNbDownloads(nbDownloads);

            // Get creator name if applicable
            if (link.getCreatedBy() != null) {
                BOUser creator = userService.findUserById(link.getCreatedBy());
                if (creator != null) {
                    dto.setCreatedByName(creator.getEmail());
                }
            }

            // Get partner name if applicable
            if (link.getPartnerId() != null) {
                try {
                    UserApi partner = userApiService.findById(link.getPartnerId());
                    dto.setPartnerName(partner.getName());
                } catch (Exception e) {
                    log.warn("Partner not found for id: {}", link.getPartnerId());
                }
            }

            String urlPath = link.isFullData() ? "/file/" : "/public-file/";
            dto.setFullUrl(tenantBaseUrl + urlPath + link.getToken());

            enrichedLinks.add(dto);
        }

        // Sort by creation date (most recent first)
        enrichedLinks.sort(Comparator.comparing(ApartmentSharingLinkEnrichedDTO::getCreationDate).reversed());

        return enrichedLinks;
    }

}
