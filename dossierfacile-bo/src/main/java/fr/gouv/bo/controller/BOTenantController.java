package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.*;
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

import java.util.*;

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
    private static final String REDIRECT_BO = "redirect:/bo";
    private static final String CUSTOM_MESSAGE = "customMessage";
    private static final String CLARIFICATION = "clarification";
    private static final String OPERATOR_COMMENT = "operatorComment";
    private static final String REDIRECT_ERROR = "redirect:/error";

    private final TenantService tenantService;
    private final MessageService messageService;
    private final DocumentService documentService;
    private final UserApiService userApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final TenantLogService logService;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;

    @GetMapping("/{id}")
    public String getTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant != null) {
            return redirectToTenantPage(tenant);
        }
        throw new NotFoundException("Tenant is not found. Still exists?");
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @GetMapping("/setAsTenantCreate/{id}")
    public String setAsTenantCreate(@PathVariable Long id) {
        Tenant tenant = userService.setAsTenantCreate(tenantService.findTenantById(id));
        return redirectToTenantPage(tenant);
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @GetMapping("/deleteCoTenant/{id}")
    public String deleteCoTenant(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant.getTenantType() == TenantType.CREATE) {
            throw new IllegalArgumentException("Delete main tenant is not allowed - set another user as main tenant before OR delete the entire apartmentSharing");
        }
        BOUser operator = userService.findUserByEmail(principal.getEmail());
        userService.deleteCoTenant(tenant, operator);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @GetMapping("/deleteApartmentSharing/{id}")
    public String deleteApartmentSharing(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Tenant create = tenantService.findTenantById(id);
        BOUser operator = userService.findUserByEmail(principal.getEmail());
        userService.deleteApartmentSharing(create, operator);
        return REDIRECT_BO;
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/partner/{id}")
    public String sendCallbackToPartner(@PathVariable("id") Long id, PartnerDTO partnerDTO) {
        Tenant tenant = tenantService.find(id);

        UserApi userApi = userApiService.findById(partnerDTO.getPartner());
        PartnerCallBackType partnerCallBackType = tenant.getStatus() == TenantFileStatus.VALIDATED ?
                PartnerCallBackType.VERIFIED_ACCOUNT :
                PartnerCallBackType.CREATED_ACCOUNT;
        ApplicationModel webhookDTO = partnerCallBackService.getWebhookDTO(tenant, userApi, partnerCallBackType);
        partnerCallBackService.sendCallBack(tenant, userApi, webhookDTO);

        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> validateTenantFile(
            @PathVariable("id") Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tenantService.validateTenantFile(principal, tenantId);
        return ok().build();
    }

    @PreAuthorize("hasRole('SUPPORT')")
    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineTenantFile(
            @PathVariable("id") Long tenantId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tenantService.declineTenant(principal, tenantId);
        return ok().build();
    }

    @PostMapping("/{id}/customMessage")
    public String customEmail(
            @PathVariable("id") Long tenantId,
            CustomMessage customMessage,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return tenantService.customMessage(principal, tenantId, customMessage);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/delete/document/{id}")
    public String deleteDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.deleteDocument(id, operator);
        return redirectToTenantPage(tenant);
    }

    @GetMapping("/status/{id}")
    public String changeStatusOfDocument(
            @PathVariable("id") Long id,
            MessageDTO messageDTO,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User operator = userService.findUserByEmail(principal.getEmail());
        Tenant tenant = tenantService.changeDocumentStatus(id, messageDTO, operator);

        return redirectToTenantPage(tenant);
    }

    @GetMapping("/{id}/processFile")
    public String processFileForm(Model model, @PathVariable("id") Long id) {
        Tenant tenant = tenantService.find(id);

        if (tenant == null) {
            log.error("BOTenantController processFile not found tenant with id : {}", id);
            return REDIRECT_ERROR;
        }
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

    @PreAuthorize("hasRole('SUPPORT')")
    @GetMapping("/delete/guarantor/{guarantorId}")
    public String deleteGuarantor(
            @PathVariable("guarantorId") Long guarantorId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
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
            CustomMessage customMessage,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tenantService.processFile(id, customMessage, principal); 
        return tenantService.redirectToApplication(principal, null);
    }

    @PostMapping("/{id}/comment")
    public String addOperatorComment(@PathVariable("id") Long tenantId,
                                     @RequestParam String comment,
                                     @RequestParam(value = "returnTo", required = false) String returnTo) {
        Tenant tenant = tenantService.addOperatorComment(tenantId, comment);
        if ("processFile".equals(returnTo)) {
            return "redirect:/bo/tenant/" + tenantId + "/processFile";
        }
        return redirectToTenantPage(tenant);
    }

    private static String redirectToTenantPage(Tenant tenant) {
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
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

    private CustomMessage getCustomMessage(Tenant tenant) {

        CustomMessage customMessage = new CustomMessage();

        List<Document> documents = tenant.getDocuments();
        documents.sort(Comparator.comparing(Document::getDocumentCategory));
        for (Document document : documents) {
            if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                customMessage.getMessageItems().add(MessageItem.builder()
                        .monthlySum(document.getMonthlySum())
                        .newMonthlySum(document.getMonthlySum())
                        .customTex(document.getCustomText())
                        .avisDetected(document.getAvisDetected())
                        .documentCategory(document.getDocumentCategory())
                        .documentSubCategory(document.getDocumentSubCategory())
                        .documentCategoryStep(document.getDocumentCategoryStep())
                        .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentCategory(), document.getDocumentSubCategory(), TENANT))
                        .documentId(document.getId())
                        .documentName(document.getName())
                        .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
                        .previousDeniedReasons(documentDeniedReasonsService.getLastDeniedReason(document, tenant).orElse(null))
                        .documentAnalysisReport(document.getDocumentAnalysisReport())
                        .analysisReportComment(document.getDocumentAnalysisReport() != null && (DocumentAnalysisStatus.DENIED == document.getDocumentAnalysisReport().getAnalysisStatus()) ? document.getDocumentAnalysisReport().getComment() : null)
                        .build());
            }
        }

        for (Guarantor guarantor : tenant.getGuarantors()) {
            GuarantorItem guarantorItem = GuarantorItem.builder()
                    .guarantorId(guarantor.getId())
                    .typeGuarantor(guarantor.getTypeGuarantor())
                    .firstName(guarantor.getFirstName())
                    .lastName(guarantor.getLastName())
                    .legalPersonName(guarantor.getLegalPersonName())
                    .build();

            documents = guarantor.getDocuments();
            documents.sort(Comparator.comparing(Document::getDocumentCategory));
            for (Document document : documents) {
                if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                    guarantorItem.getMessageItems().add(MessageItem.builder()
                            .monthlySum(document.getMonthlySum())
                            .newMonthlySum(document.getMonthlySum())
                            .avisDetected(document.getAvisDetected())
                            .customTex(document.getCustomText())
                            .documentCategory(document.getDocumentCategory())
                            .documentSubCategory(document.getDocumentSubCategory())
                            .documentCategoryStep(document.getDocumentCategoryStep())
                            .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentCategory(), document.getDocumentSubCategory(), GUARANTOR))
                            .documentId(document.getId())
                            .documentName(document.getName())
                            .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
                            .documentAnalysisReport(document.getDocumentAnalysisReport())
                            .analysisReportComment(document.getDocumentAnalysisReport() != null && (DocumentAnalysisStatus.DENIED == document.getDocumentAnalysisReport().getAnalysisStatus()) ? document.getDocumentAnalysisReport().getComment() : null)
                            .previousDeniedReasons(documentDeniedReasonsService.getLastDeniedReason(document, tenant).orElse(null))
                            .build());
                }
            }
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
