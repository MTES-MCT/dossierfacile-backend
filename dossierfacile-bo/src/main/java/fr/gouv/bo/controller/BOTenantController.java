package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.MessageStatus;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.CustomMessage;
import fr.gouv.bo.dto.DisplayableFile;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.dto.GuarantorItem;
import fr.gouv.bo.dto.ItemDetail;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.MessageItem;
import fr.gouv.bo.dto.PartnerDTO;
import fr.gouv.bo.dto.TenantInfoHeader;
import fr.gouv.bo.service.ApartmentSharingService;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.GuarantorService;
import fr.gouv.bo.service.LogService;
import fr.gouv.bo.service.MessageService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.TenantUserApiService;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/bo/tenant")
@Slf4j
public class BOTenantController {

    private static final String EMAIL = "email";
    private static final String TENANT = "tenant";
    private static final String GUARANTOR = "guarantor";
    private static final String NEW_MESSAGE = "newMessage";
    private static final String HEADER = "header";
    private static final String REDIRECT_BO = "redirect:/bo";
    private static final String CUSTOM_MESSAGE = "customMessage";
    private static final String REDIRECT_ERROR = "redirect:/error";

    private final TenantService tenantService;
    private final MessageService messageService;
    private final GuarantorService guarantorService;
    private final DocumentService documentService;
    private final UserApiService userApiService;
    private final TenantUserApiService tenantUserApiService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final ApartmentSharingService apartmentSharingService;
    private final LogService logService;

    @Value("${bo.message-tenant.location}")
    String locationMessageTenant;
    @Value("${bo.message-guarantor.location}")
    String locationMessageGuarantor;

    @GetMapping("/{id}")
    public String getTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant != null) {
            return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
        }
        throw new ObjectNotFoundException("TENANT", "Tenant is not found. Still exists?");
    }

    @GetMapping("/setAsTenantCreate/{id}")
    public String setAsTenantCreate(@PathVariable Long id) {
        Tenant tenant = userService.setAsTenantCreate(tenantService.findTenantById(id));
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    @GetMapping("/deleteCoTenant/{id}")
    public String deleteCoTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findTenantById(id);
        if (tenant.getTenantType() == TenantType.CREATE) {
            throw new IllegalArgumentException("Delete main tenant is not allowed - set another user as main tenant before OR delete the entire apartmentSharing");
        }
        userService.deleteCoTenant(tenant);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @GetMapping("/deleteApartmentSharing/{id}")
    public String deleteApartmentSharing(@PathVariable("id") Long id) {
        Tenant create = tenantService.findTenantById(id);
        userService.deleteApartmentSharing(create);
        return REDIRECT_BO;
    }

    @GetMapping("/partner/{id}")
    public String addNewPartnerInfo(@PathVariable("id") Long id, PartnerDTO partnerDTO) {

        Tenant tenant = tenantService.find(id);

        UserApi userApi = userApiService.findById(partnerDTO.getPartner());
        tenantUserApiService.addInternalPartnerIdToTenantUserApi(tenant, partnerDTO.getPartner(), partnerDTO.getInternalPartnerId());
        partnerCallBackService.sendCallBack(tenant, userApi, tenant.getStatus() == TenantFileStatus.VALIDATED ?
                PartnerCallBackType.VERIFIED_ACCOUNT :
                PartnerCallBackType.CREATED_ACCOUNT);

        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId();
    }

    @GetMapping("/{id}/showResult")
    public String showResult(Model model, @PathVariable("id") Long id) {
        Tenant tenant = tenantService.find(id);
        model.addAttribute(TENANT, tenant);
        return "include/process-files-result:: process-files-result";
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Void> validateTenantFile(@PathVariable("id") Long tenantId, Principal principal) {
        tenantService.validateTenantFile(principal, tenantId);
        return ok().build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineTenantFile(@PathVariable("id") Long tenantId, Principal principal) {
        tenantService.declineTenant(principal, tenantId);
        return ok().build();
    }

    @GetMapping("/{id}/customMessage")
    public String customEmailForm(@PathVariable("id") Long id, Model model) throws IOException {
        Tenant tenant = tenantService.find(id);
        if (tenant == null) {
            log.error("BOTenantController customEmailForm not found tenant with id {}", id);
            return REDIRECT_ERROR;
        }

        model.addAttribute("customMessage", getCustomMessage(tenant));
        model.addAttribute(TENANT, tenant);
        return "bo/tenant-custom-message-form";
    }

    @PostMapping("/{id}/customMessage")
    public String customEmail(@PathVariable("id") Long tenantId, CustomMessage customMessage, Principal principal) {
        return tenantService.customMessage(principal, tenantId, customMessage);
    }

    @GetMapping("/delete/document/{id}")
    public String deleteDocument(@PathVariable("id") Long id, Principal principal) {
        Tenant tenant = documentService.deleteDocument(id);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        User operator = userService.findUserByEmail(principal.getName());
        tenantService.updateTenantStatus(tenant, operator);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    @GetMapping("/status/{id}")
    public String changeStatusOfDocument(@PathVariable("id") Long id, MessageDTO messageDTO, Principal principal) {
        Tenant tenant = documentService.changeStatusOfDocument(id, messageDTO);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        User operator = userService.findUserByEmail(principal.getName());
        tenantService.updateTenantStatus(tenant, operator);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    @GetMapping("/{id}/processFile")
    public String processFileForm(Model model, @PathVariable("id") Long id) throws IOException {
        Tenant tenant = tenantService.find(id);

        if (tenant == null) {
            log.error("BOTenantController processFile not found tenant with id : {}", id);
            return REDIRECT_ERROR;
        }
        TenantInfoHeader header = TenantInfoHeader.build(tenant,
                userApiService.findPartnersLinkedToTenant(id),
                logService.getLogByTenantId(id));
        EmailDTO emailDTO = new EmailDTO();
        model.addAttribute(EMAIL, emailDTO);
        model.addAttribute(HEADER, header);
        model.addAttribute(NEW_MESSAGE, findNewMessageFromTenant(id));
        model.addAttribute(TENANT, tenant);
        model.addAttribute(CUSTOM_MESSAGE, getCustomMessage(tenant));
        return "bo/process-file";
    }

    @GetMapping("/delete/guarantor/{guarantorId}")
    public String deleteGuarantor(@PathVariable("guarantorId") Long guarantorId, Principal principal) {
        Tenant tenant = guarantorService.deleteById(guarantorId);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        User operator = userService.findUserByEmail(principal.getName());
        tenantService.updateTenantStatus(tenant, operator);
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
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
    public String processFile(@PathVariable("id") Long id, CustomMessage customMessage, Principal principal) {
        tenantService.processFile(id, customMessage, principal);
        tenantService.updateOperatorDateTimeTenant(id);
        return tenantService.redirectToApplication(principal, null);
    }

    private List<ItemDetail> getItemDetailForSubcategoryOfDocument(DocumentSubCategory documentSubCategory, String tenantOrGuarantor) {

        List<ItemDetail> itemDetails = new ArrayList<>();
        for (DocumentDeniedOptions documentDeniedOptions : documentService.findDocumentDeniedOptionsByDocumentSubCategoryAndDocumentUserType(documentSubCategory, tenantOrGuarantor)) {
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
                        .customTex(document.getCustomText())
                        .taxDocument(document.getTaxProcessResult())
                        .avisDetected(document.getAvisDetected())
                        .documentCategory(document.getDocumentCategory())
                        .documentSubCategory(document.getDocumentSubCategory())
                        .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentSubCategory(), TENANT))
                        .documentId(document.getId())
                        .documentName(document.getName())
                        .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
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
                            .customTex(document.getCustomText())
                            .taxDocument(document.getTaxProcessResult())
                            .documentCategory(document.getDocumentCategory())
                            .documentSubCategory(document.getDocumentSubCategory())
                            .itemDetailList(getItemDetailForSubcategoryOfDocument(document.getDocumentSubCategory(), GUARANTOR))
                            .documentId(document.getId())
                            .documentName(document.getName())
                            .analyzedFiles(DisplayableFile.onlyAnalyzedFilesOf(document))
                            .build());
                }
            }
            customMessage.getGuarantorItems().add(guarantorItem);
        }
        return customMessage;
    }

}
