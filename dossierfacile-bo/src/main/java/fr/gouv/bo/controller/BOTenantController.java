package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.*;
import fr.gouv.bo.security.BOAccessDenied;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static fr.gouv.bo.controller.BOController.REDIRECT_BO_HOME;
import static org.springframework.http.ResponseEntity.ok;

@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/bo/tenant")
@Slf4j
public class BOTenantController {

    private static final String LOGSER = "logser";
    private static final String MODIFICATION_LOGS = "modificationLogs";
    private static final String TENANT = "tenant";
    private static final String GUARANTOR = "guarantor";
    private static final String NEW_MESSAGE = "newMessage";
    private static final String HEADER = "header";
    private static final String DOCUMENT_RULE_LEVEL = "documentRuleLevel";
    private static final String REDIRECT_BO_COLOCATION = "redirect:/bo/colocation/";
    private static final String CUSTOM_MESSAGE = "customMessage";
    private static final String CLARIFICATION = "clarification";
    private static final String OPERATOR_COMMENT = "operatorComment";

    private final TenantService tenantService;
    private final MessageService messageService;
    private final DocumentService documentService;
    private final UserApiService userApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final TenantLogService logService;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;
    private final BOApplicationAccessService applicationAccessService;
    private final BOTenantResolver tenantResolver;

    @GetMapping("/{id}")
    public String getTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant tenant = requireTenant(id);
        applicationAccessService.checkTenantAccess(principal, tenant.getId());
        return redirectToTenantPage(tenant);
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @DeleteMapping("/deleteCoTenant/{id}")
    public String deleteCoTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant tenant = requireTenant(id);
        if (tenant.getTenantType() == TenantType.CREATE) {
            throw new IllegalArgumentException("Delete main tenant is not allowed - set another user as main tenant before OR delete the entire apartmentSharing");
        }
        BOUser operator = userService.findUserByEmail(principal.getEmail());
        userService.deleteCoTenant(tenant, operator);
        return REDIRECT_BO_COLOCATION + tenant.getApartmentSharing().getId();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/dissociate/{id}")
    public String dissociateTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        Tenant tenant = requireTenant(id);
        BOUser operator = userService.findUserByEmail(principal.getEmail());
        Long apartmentSharingId = tenant.getApartmentSharing().getId();
        try {
            Long newApartmentSharingId = tenantService.dissociateTenant(tenant, operator);
            return REDIRECT_BO_COLOCATION + newApartmentSharingId + "#tenant" + tenant.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return REDIRECT_BO_COLOCATION + apartmentSharingId + "#tenant" + tenant.getId();
        }
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @DeleteMapping("/deleteApartmentSharing/{id}")
    public String deleteApartmentSharing(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant create = requireTenant(id);
        BOUser operator = userService.findUserByEmail(principal.getEmail());
        userService.deleteApartmentSharing(create, operator);
        return REDIRECT_BO_HOME;
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/partner/{id}")
    public String sendCallbackToPartner(@PathVariable("id") Long id, PartnerDTO partnerDTO) {
        Tenant tenant = requireTenant(id);

        UserApi userApi = userApiService.findById(partnerDTO.getPartner());
        PartnerCallBackType partnerCallBackType = tenant.getStatus() == TenantFileStatus.VALIDATED ?
                PartnerCallBackType.VERIFIED_ACCOUNT :
                PartnerCallBackType.CREATED_ACCOUNT;
        ApplicationModel webhookDTO = partnerCallBackService.getWebhookDTO(tenant, userApi, partnerCallBackType);
        partnerCallBackService.sendCallBack(tenant, userApi, webhookDTO);

        return REDIRECT_BO_COLOCATION + tenant.getApartmentSharing().getId();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> validateTenantFile(
            @PathVariable("id") Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireTenant(tenantId);
        tenantService.validateTenantFile(principal, tenantId);
        return ok().build();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineTenantFile(
            @PathVariable("id") Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireTenant(tenantId);
        tenantService.declineTenant(principal, tenantId);
        return ok().build();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<Void> reprocessTenantFile(
            @PathVariable("id") Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireTenant(tenantId);
        tenantService.reprocessTenant(principal, tenantId);
        return ok().build();
    }

    @PostMapping("/{id}/customMessage")
    public String customEmail(
            @PathVariable("id") Long tenantId,
            CustomMessage customMessage,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant tenant = requireTenant(tenantId);
        applicationAccessService.checkTenantAccess(principal, tenant.getId());
        return tenantService.customMessage(principal, tenantId, customMessage);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @DeleteMapping("/delete/document/{id}")
    public String deleteDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant accessTenant = tenantResolver.resolveTenantFromDocument(id);
        applicationAccessService.checkTenantAccess(principal, accessTenant.getId());
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.deleteDocument(id, operator);
        return redirectToTenantPage(tenant);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @DeleteMapping("/delete/file/{id}")
    public String deleteFile(
            @PathVariable("id") Long fileId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant accessTenant = tenantResolver.resolveTenantFromFile(fileId);
        applicationAccessService.checkTenantAccess(principal, accessTenant.getId());
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.deleteFile(fileId, operator);
        return redirectToTenantPage(tenant);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @PostMapping("/status/{id}")
    public String changeStatusOfDocument(
            @PathVariable("id") Long id,
            MessageDTO messageDTO,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant accessTenant = tenantResolver.resolveTenantFromDocument(id);
        applicationAccessService.checkTenantAccess(principal, accessTenant.getId());
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.changeDocumentStatus(id, messageDTO, operator);

        return redirectToTenantPage(tenant);
    }

    @GetMapping("/{id}/processFile")
    public String processFileForm(Model model, @PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        
        Tenant tenant = requireTenant(id);                            
        applicationAccessService.checkTenantAccess(principal, tenant.getId());
        
        List<TenantLog> logs = logService.getLogByTenantId(id);
        TenantInfoHeader header = TenantInfoHeader.build(tenant, userApiService.findPartnersLinkedToTenant(id), logs);
        List<TenantLog> modificationLogs = logs.stream()
                .filter(l -> l.getLogDetails() != null && l.getLogDetails().get("newSum") != null)
                .toList();
        model.addAttribute(MODIFICATION_LOGS, modificationLogs);
        model.addAttribute(LOGSER, logService);
        model.addAttribute(HEADER, header);
        model.addAttribute(NEW_MESSAGE, findNewMessageFromTenant(id));
        model.addAttribute(TENANT, tenant);
        model.addAttribute(DOCUMENT_RULE_LEVEL, DocumentRuleLevel.WARN);
        model.addAttribute(CUSTOM_MESSAGE, getCustomMessage(tenant));
        model.addAttribute(CLARIFICATION, splitInParagraphs(tenant.getClarification()));
        model.addAttribute(OPERATOR_COMMENT, splitInParagraphs(tenant.getOperatorComment()));
        return "bo/process-file";
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @DeleteMapping("/delete/guarantor/{guarantorId}")
    public String deleteGuarantor(
            @PathVariable("guarantorId") Long guarantorId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant accessTenant = tenantResolver.resolveTenantFromGuarantor(guarantorId);
        applicationAccessService.checkTenantAccess(principal, accessTenant.getId());
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.deleteGuarantor(guarantorId, operator);
        return redirectToTenantPage(tenant);
    }

    private Boolean findNewMessageFromTenant(Long id) {

        User tenant1 = tenantService.getUserById(id);
        List<Message> messages = messageService.findTenantMessages(tenant1);
        for (Message message : messages) {
            if (message.getMessageStatus().equals(MessageStatus.UNREAD) && message.getFromUser() != null)
                return true;
        }
        return false;
    }

    @PostMapping("/{id}/processFile")
    public String processFile(
            @PathVariable Long id,
            @RequestParam(value = "returnToHome", required = false) String returnToHome,
            CustomMessage customMessage,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant tenant = requireTenant(id);
        applicationAccessService.checkTenantAccess(principal, tenant.getId());
        
        tenantService.processFile(id, customMessage, principal);

        // Si returnToHome est demandé, retourner à l'accueil
        if (returnToHome != null && returnToHome.equals("true")) {
            return REDIRECT_BO_HOME;
        }

        // Trouver le prochain tenant du même dossier COUPLE en statut TO_PROCESS
        // pour router les locataires d'un meme dossier COUPLE vers le même opérateur
        Optional<Tenant> nextTenantInCouple = tenantService.findNextTenantInCouple(id);
        if (nextTenantInCouple.isPresent()) {
            // Rediriger vers le prochain locataire du dossier
            return tenantService.redirectToApplication(principal, nextTenantInCouple.get().getId());
        }

        // Sinon, continuer avec le comportement normal (prochain dossier disponible)
        return tenantService.redirectToApplication(principal, null);
    }

    @PostMapping("/{id}/comment")
    public String addOperatorComment(@PathVariable("id") Long tenantId,
                                     @RequestParam String comment,
                                     @RequestParam(value = "returnTo", required = false) String returnTo,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        
        Tenant tenant = requireTenant(tenantId);
        applicationAccessService.checkTenantAccess(principal, tenant.getId());
        
        tenantService.addOperatorComment(principal, tenant.getId(), comment);
        if ("processFile".equals(returnTo)) {
            return REDIRECT_BO_HOME + "/tenant/" + tenant.getId() + "/processFile";
        }
        return redirectToTenantPage(tenant);
    }

    private Tenant requireTenant(Long tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant == null) {
            log.warn("BO access denied: tenant not found id={}", tenantId);
            throw BOAccessDenied.generic();
        }
        return tenant;
    }

    private static String redirectToTenantPage(Tenant tenant) {
        return REDIRECT_BO_COLOCATION + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    private List<ItemDetail> getItemDetailForSubcategoryOfDocument(DocumentCategory documentCategory, DocumentSubCategory documentSubCategory, String documentUserType) {

        List<ItemDetail> itemDetails = new ArrayList<>();
        for (DocumentDeniedOptions documentDeniedOptions : documentService.findDocumentDeniedOptionsByDocumentSubCategoryAndDocumentUserTypeIncludeGeneric(documentCategory, documentSubCategory, documentUserType)) {
            ItemDetail itemDetail1 = ItemDetail.builder()
                    .check(false)
                    .message(documentDeniedOptions.getMessageValue())
                    .idOptionMessage(documentDeniedOptions.getId())
                    .build();
            itemDetails.add(itemDetail1);
        }
        return itemDetails;
    }

    private List<MessageItem> getMessageItemsForDocuments(List<Document> documents, Tenant tenant, boolean isForGuarantor) {
        List<MessageItem> messageItems = new ArrayList<>();

        for (Document document : documents) {
            if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                var messageItemBuilder = MessageItem.builder();
                messageItemBuilder
                        .monthlySum(document.getMonthlySum())
                        .newMonthlySum(document.getMonthlySum())
                        .customTex(document.getCustomText())
                        .avisDetected(document.getAvisDetected())
                        .documentCategory(document.getDocumentCategory())
                        .documentSubCategory(document.getDocumentSubCategory())
                        .documentCategoryStep(document.getDocumentCategoryStep())
                        .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentCategory(), document.getDocumentSubCategory(), isForGuarantor ? GUARANTOR : TENANT))
                        .documentId(document.getId())
                        .documentName(document.getName())
                        .noDocument(document.getNoDocument())
                        .metadataOfFiles(documentService.getFilesMetadata(document))
                        .previousDeniedReasons(documentDeniedReasonsService.getLastDeniedReason(document, tenant).orElse(null))
                        .documentAnalysisReport(document.getDocumentAnalysisReport())
                        .analysisReportComment(document.getDocumentAnalysisReport() != null && (DocumentAnalysisStatus.DENIED == document.getDocumentAnalysisReport().getAnalysisStatus()) ? document.getDocumentAnalysisReport().getComment() : null);

                var resultList = document.getFiles().stream().filter(
                        it -> it.getDocumentIAFileAnalysis() != null && it.getDocumentIAFileAnalysis().getAnalysisStatus() == DocumentIAFileAnalysisStatus.SUCCESS
                ).map(
                        file -> file.getDocumentIAFileAnalysis().getResult()
                ).toList();

                messageItemBuilder.documentIAResults(resultList);
                messageItems.add(messageItemBuilder.build());
            }
        }
        return messageItems;
    }

    private CustomMessage getCustomMessage(Tenant tenant) {

        CustomMessage customMessage = new CustomMessage();

        List<Document> documents = tenant.getDocuments();
        documents.sort(Comparator.comparing(Document::getDocumentCategory));

        customMessage.setMessageItems(getMessageItemsForDocuments(documents, tenant, false));

        for (Guarantor guarantor : tenant.getGuarantors()) {
            GuarantorItem guarantorItem = GuarantorItem.builder()
                    .guarantorId(guarantor.getId())
                    .typeGuarantor(guarantor.getTypeGuarantor())
                    .firstName(guarantor.getFirstName())
                    .lastName(guarantor.getLastName())
                    .preferredName(guarantor.getPreferredName())
                    .legalPersonName(guarantor.getLegalPersonName())
                    .build();

            documents = guarantor.getDocuments();
            documents.sort(Comparator.comparing(Document::getDocumentCategory));
            guarantorItem.setMessageItems(getMessageItemsForDocuments(documents, tenant, true));
            customMessage.getGuarantorItems().add(guarantorItem);
        }

        return customMessage;
    }

    private static List<String> splitInParagraphs(String string) {
        if (string == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(string.split("\n"))
                .filter(paragraph -> !paragraph.isBlank())
                .toList();
    }

}
