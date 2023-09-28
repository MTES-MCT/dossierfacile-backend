package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.WebhookDTO;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.dto.CustomMessage;
import fr.gouv.bo.dto.EmailDTO;
import fr.gouv.bo.dto.GuarantorItem;
import fr.gouv.bo.dto.ItemDetail;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.dto.MessageItem;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.lambda_interfaces.StringCustomMessage;
import fr.gouv.bo.lambda_interfaces.StringCustomMessageGuarantor;
import fr.gouv.bo.model.ProcessedDocuments;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.repository.UserApiRepository;
import fr.gouv.bo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private static final String LI_P = "<li><p>";
    private static final String P_LI = "</p></li>";
    private static final String BOLD_CLOSE = "</b> ";

    private final Locale locale = LocaleContextHolder.getLocale();
    private final TenantCommonRepository tenantRepository;
    private final MailService mailService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final DocumentRepository documentRepository;
    private final DocumentDeniedReasonsRepository documentDeniedReasonsRepository;
    private final UserApiRepository userApiRepository;
    private final MessageService messageService;
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final OperatorLogRepository operatorLogRepository;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;
    private final DocumentService documentService;
    private final LogService logService;
    private final KeycloakService keycloakService;

    private int forTenant = 0;
    @Value("${time.reprocess.application.minutes}")
    private int timeReprocessApplicationMinutes;

    @Value("${specialized.operator.email:ops@dossiefacile.fr}")
    private String specializedOperatorEmail;

    @Value("${process.max.dossier.time.interval:10}")
    private Long timeInterval;
    @Value("${process.max.dossier.by.interval:20}")
    private Long maxDossiersByInterval;
    @Value("${process.max.dossier.by.day:600}")
    private Long maxDossiersByDay;

    public List<Tenant> getTenantByIdOrEmail(EmailDTO emailDTO) {
        List<Tenant> tenantList = new ArrayList<>();
        if (StringUtils.isNumeric(emailDTO.getEmail())) {
            tenantList.add(tenantRepository.findOneById(Long.parseLong(emailDTO.getEmail())));
            return tenantList;
        }
        if (emailDTO.getEmail().contains("@")) {
            tenantList.add(tenantRepository.findByEmailIgnoreCase(emailDTO.getEmail()).get());
            tenantList.add(Tenant.builder().build());
            return tenantList;
        }
        return searchTenantByName(emailDTO);
    }

    public List<Tenant> searchTenantByName(EmailDTO emailDTO) {
        return tenantRepository.findTenantByFirstNameOrLastNameOrFullName(emailDTO.getEmail().toLowerCase(Locale.ROOT));
    }

    public Tenant findTenantById(Long id) {
        return tenantRepository.findOneById(id);
    }

    public User getUserById(Long id) {
        return tenantRepository.getOne(id);
    }

    public Tenant getTenantById(Long id) {
        return tenantRepository.findOneById(id);
    }

    public Page<Tenant> listTenantsToProcess(Pageable pageable) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(timeReprocessApplicationMinutes);
        return new PageImpl<>(tenantRepository.findTenantsToProcess(localDateTime, pageable).toList());
    }

    public Page<Tenant> listTenantsFilter(Pageable pageable, String q) {
        return tenantRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(q, q, q, pageable);
    }

    public Tenant find(Long id) {
        return tenantRepository.findOneById(id);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public Boolean hasVerifiedEmailIfExistsInKeycloak(Tenant tenant) {
        UserRepresentation keyCloakUser = keycloakService.getKeyCloakUser(tenant.getKeycloakId());
        if (keyCloakUser == null) {
            return null;
        }
        return keyCloakUser.isEmailVerified();
    }

    public synchronized String redirectToApplication(Principal principal, Long tenantId) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(timeReprocessApplicationMinutes);
        Tenant tenant;
        if (tenantId == null) {
            UserPrincipal operator = (UserPrincipal) ((OAuth2AuthenticationToken) principal).getPrincipal();
            Long operatorId = operator.getId();
            // check less than x process are currently starting during the n lastMinutes
            if (operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(operatorId, ActionOperatorType.START_PROCESS, LocalDateTime.now().minusMinutes(timeInterval)) > maxDossiersByInterval) {
                throw new IllegalStateException("Vous ne pouvez pas ouvrir plus de " + maxDossiersByInterval + " dossiers pour traitement toutes les " + timeInterval + " minutes");
            }
            if (operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(operatorId, ActionOperatorType.START_PROCESS, LocalDateTime.now().toLocalDate().atStartOfDay()) > maxDossiersByDay) {
                throw new IllegalStateException("Vous ne pouvez pas ouvrir plus de " + maxDossiersByDay + " dossiers par jour");
            }

            List<String> specializedOperators = Arrays.asList(specializedOperatorEmail.split(","));
            if (specializedOperators.contains(operator.getEmail())) {
                tenant = tenantRepository.findNextApplicationByProfessional(localDateTime, Arrays.asList(DocumentSubCategory.CDI, DocumentSubCategory.PUBLIC));
                if (tenant == null) {
                    tenant = tenantRepository.findNextApplication(localDateTime);
                }
            } else {
                tenant = tenantRepository.findNextApplication(localDateTime);
            }
        } else {
            tenant = find(tenantId);
        }

        if (tenant != null) {
            User user = userService.findUserByEmail(principal.getName());
            operatorLogRepository.save(new OperatorLog(
                    tenant, user, tenant.getStatus(), ActionOperatorType.START_PROCESS
            ));
            updateOperatorDateTimeTenant(tenant.getId());
            return "redirect:/bo/tenant/" + tenant.getId() + "/processFile";
        } else {
            return "redirect:/bo";
        }
    }

    public List<Tenant> findAllTenantsByApartmentSharingAndReorderDocumentsByCategory(Long id) {
        List<Tenant> tenants = tenantRepository.findAllByApartmentSharingId(id);
        for (Tenant tenant : tenants) {
            tenant.getDocuments().sort(Comparator.comparing(Document::getDocumentCategory));
            for (Guarantor guarantor : tenant.getGuarantors()) {
                guarantor.getDocuments().sort(Comparator.comparing(Document::getDocumentCategory));
            }
        }
        tenants.sort(Comparator.comparing(Tenant::getTenantType));
        return tenants;
    }

    @Transactional
    public void validateTenantFile(Principal principal, Long tenantId) {
        Tenant tenant = find(tenantId);
        BOUser operator = userService.findUserByEmail(principal.getName());

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    document.setDocumentStatus(DocumentStatus.VALIDATED);
                    document.setDocumentDeniedReasons(null);
                    documentRepository.save(document);
                });
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(document -> {
                            document.setDocumentStatus(DocumentStatus.VALIDATED);
                            document.setDocumentDeniedReasons(null);
                            documentRepository.save(document);
                        }));
        changeTenantStatusToValidated(tenant, operator, ProcessedDocuments.NONE);
    }

    private boolean updateFileStatus(CustomMessage customMessage) {
        List<MessageItem> messageItems = customMessage.getMessageItems();

        boolean areAllDocumentsValid = true;
        for (MessageItem messageItem : messageItems) {

            ItemDetail messageItem1 = messageItem.getItemDetailList().stream().filter(ItemDetail::isCheck).findAny().orElse(null);

            if (messageItem1 == null && messageItem.getCommentDoc().isEmpty()) {
                documentRepository.findById(messageItem.getDocumentId()).
                        ifPresent(d -> {
                            d.setDocumentStatus(DocumentStatus.VALIDATED);
                            d.setDocumentDeniedReasons(null);
                            documentRepository.save(d);
                        });
            } else {
                areAllDocumentsValid = false;
                documentRepository.findById(messageItem.getDocumentId()).
                        ifPresent(d -> {
                            d.setDocumentStatus(DocumentStatus.DECLINED);
                            documentRepository.save(d);
                        });
            }

        }
        List<GuarantorItem> guarantorItems = customMessage.getGuarantorItems();
        for (GuarantorItem guarantorItem : guarantorItems) {
            messageItems = guarantorItem.getMessageItems();
            for (MessageItem messageItem : messageItems) {

                ItemDetail messageItem1 = messageItem.getItemDetailList().stream().filter(ItemDetail::isCheck).findAny().orElse(null);

                if (messageItem1 == null && messageItem.getCommentDoc().isEmpty()) {
                    documentRepository.findById(messageItem.getDocumentId()).
                            ifPresent(d -> {
                                d.setDocumentStatus(DocumentStatus.VALIDATED);
                                d.setDocumentDeniedReasons(null);
                                documentRepository.save(d);
                            });
                } else {
                    areAllDocumentsValid = false;
                    documentRepository.findById(messageItem.getDocumentId()).
                            ifPresent(d -> {
                                d.setDocumentStatus(DocumentStatus.DECLINED);
                                documentRepository.save(d);
                            });
                }
            }
        }
        return areAllDocumentsValid;
    }

    public Message sendCustomMessage(Tenant tenant, CustomMessage customMessage, int messageFrom) {
        StringCustomMessage fileNameWithBold = str -> "<b>" + messageSource.getMessage(str, null, locale) + BOLD_CLOSE;
        StringCustomMessageGuarantor fileNameWithBoldGuarantor = str -> "<b>" + messageSource.getMessage(str, null, locale) + " du garant</b> ";

        List<MessageItem> messageItems = customMessage.getMessageItems();
        StringBuilder mailMessage = new StringBuilder();
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.head1", null, locale));
        mailMessage.append("<br/>");
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.head2", null, locale));
        mailMessage.append("<br/> <ul class='customMessage'>");

        List<Long> documentDeniedReasonsIds = new ArrayList<>();
        if (messageFrom == 7 || messageFrom == 2) {
            for (MessageItem messageItem : messageItems) {
                DocumentDeniedReasons documentDeniedReasons = new DocumentDeniedReasons();
                for (ItemDetail itemDetail : messageItem.getItemDetailList()) {
                    if (itemDetail.isCheck()) {
                        mailMessage.append(LI_P);
                        mailMessage.append(fileNameWithBold.getFileNameWithBold(messageItem.getDocumentCategory().getLabel()));
                        mailMessage.append(itemDetail.getFormattedMessage());
                        mailMessage.append(P_LI);
                        documentDeniedReasons.getCheckedOptions().add(itemDetail.getFormattedMessage());
                        documentDeniedReasons.getCheckedOptionsId().add(itemDetail.getIdOptionMessage());
                    }
                }

                if (!messageItem.getCommentDoc().isEmpty()) {
                    mailMessage.append(LI_P);
                    mailMessage.append(fileNameWithBold.getFileNameWithBold(messageItem.getDocumentCategory().getLabel()));
                    mailMessage.append(messageItem.getCommentDoc());
                    mailMessage.append(P_LI);
                    documentDeniedReasons.setComment(messageItem.getCommentDoc());
                }

                if (!documentDeniedReasons.getCheckedOptionsId().isEmpty() || (documentDeniedReasons.getComment() != null && !documentDeniedReasons.getComment().isBlank())) {
                    Document document = documentRepository.findById(messageItem.getDocumentId()).orElseThrow(() -> new DocumentNotFoundException(messageItem.getDocumentId()));
                    documentDeniedReasons.setDocument(document);
                    documentDeniedReasonsRepository.save(documentDeniedReasons);
                    DocumentDeniedReasons documentDeniedReasonsToDelete = document.getDocumentDeniedReasons();
                    documentService.updateDocumentWithDocumentDeniedReasons(documentDeniedReasons, messageItem.getDocumentId());
                    if (documentDeniedReasonsToDelete != null) {
                        documentDeniedReasonsRepository.delete(documentDeniedReasonsToDelete);
                    }
                    documentDeniedReasonsIds.add(documentDeniedReasons.getId());
                }
            }
        }

        if (messageFrom == 7 || messageFrom == 5) {
            for (GuarantorItem guarantorItem : customMessage.getGuarantorItems()) {
                messageItems = guarantorItem.getMessageItems();
                mailMessage.append("</ul>");
                mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.checkGuarantor", null, locale));
                mailMessage.append("<ul class='customMessage'>");
                for (MessageItem messageItem : messageItems) {
                    DocumentDeniedReasons documentDeniedReasons = new DocumentDeniedReasons();
                    for (ItemDetail itemDetail : messageItem.getItemDetailList()) {
                        if (itemDetail.isCheck()) {
                            mailMessage.append(LI_P);
                            mailMessage.append(fileNameWithBoldGuarantor.getFileNameWithBold(messageItem.getDocumentCategory().getLabel()));
                            mailMessage.append(itemDetail.getFormattedMessage());
                            mailMessage.append(P_LI);
                            documentDeniedReasons.getCheckedOptions().add(itemDetail.getMessage());
                            documentDeniedReasons.getCheckedOptionsId().add(itemDetail.getIdOptionMessage());
                        }
                    }

                    if (!messageItem.getCommentDoc().isEmpty()) {
                        mailMessage.append(LI_P);
                        mailMessage.append(fileNameWithBoldGuarantor.getFileNameWithBold(messageItem.getDocumentCategory().getLabel()));
                        mailMessage.append(messageItem.getCommentDoc());
                        mailMessage.append(P_LI);
                        documentDeniedReasons.setComment(messageItem.getCommentDoc());
                    }

                    if (!documentDeniedReasons.getCheckedOptionsId().isEmpty() || (documentDeniedReasons.getComment() != null && !documentDeniedReasons.getComment().isBlank())) {
                        Document document = documentRepository.findById(messageItem.getDocumentId()).orElseThrow(() -> new DocumentNotFoundException(messageItem.getDocumentId()));
                        documentDeniedReasons.setDocument(document);
                        documentDeniedReasonsRepository.save(documentDeniedReasons);
                        DocumentDeniedReasons documentDeniedReasonsToDelete = document.getDocumentDeniedReasons();
                        documentService.updateDocumentWithDocumentDeniedReasons(documentDeniedReasons, messageItem.getDocumentId());
                        if (documentDeniedReasonsToDelete != null) {
                            documentDeniedReasonsRepository.delete(documentDeniedReasonsToDelete);
                        }
                        documentDeniedReasonsIds.add(documentDeniedReasons.getId());
                    }
                }
            }
        }

        mailMessage.append("</ul><br/><p>");
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.footer1", null, locale));
        mailMessage.append("</p><br/><p>");
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.footer2", null, locale));
        mailMessage.append("</p><br/><p>");
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.footer3", null, locale));
        mailMessage.append("</p><p>");
        mailMessage.append(messageSource.getMessage("bo.tenant.custom.email.footer4", null, locale));
        mailMessage.append("</p>");
        if (messageFrom > 0) {
            Message message = messageService.create(new MessageDTO(mailMessage.toString()), tenant, false, true);
            if (!documentDeniedReasonsIds.isEmpty()) {
                documentDeniedReasonsService.updateDocumentDeniedReasonsWithMessage(message, documentDeniedReasonsIds);
            }
            return message;
        }
        return null;
    }

    public void declineTenant(Principal principal, Long tenantId) {
        Tenant tenant = find(tenantId);
        User operator = userService.findUserByEmail(principal.getName());

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    document.setDocumentStatus(DocumentStatus.DECLINED);
                    documentRepository.save(document);
                });
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(document -> {
                            document.setDocumentStatus(DocumentStatus.DECLINED);
                            documentRepository.save(document);
                        }));
        changeTenantStatusToDeclined(tenant, operator, null, ProcessedDocuments.NONE);
    }

    public void sendCallBacksManuallyToUserApi(Long userApiId, LocalDateTime since) {
        synchronized (this) {
            UserApi userApi = userApiRepository.findOneById(userApiId).orElseThrow(NotFoundException::new);

            //Finding the IDs of tenants pending to send info for partner with ID 2.
            List<Long> ids = tenantRepository.listIdTenantsAccountCompletedPendingToSendCallBack(userApiId, since);
            int numberOfTotalCalls = ids.size();
            log.info(numberOfTotalCalls + " tenants pending to send the validation information to the partner.");
            int indexCall = 1;
            for (Long id : ids) {
                Tenant tenant = tenantRepository.findOneById(id);
                WebhookDTO webhookDTO = partnerCallBackService.getWebhookDTO(tenant, userApi, PartnerCallBackType.VERIFIED_ACCOUNT);
                partnerCallBackService.sendCallBack(tenant, webhookDTO);

                if (indexCall < numberOfTotalCalls) {
                    try {
                        indexCall++;
                        log.info("Waiting 50ms for the next call...");
                        this.wait(50); //It will wait 3 minutes to send the next callback
                    } catch (InterruptedException e) {
                        log.error("InterruptedException sendCallBacksManually ", e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    @Transactional
    public void computeStatusOfAllTenants() {
        int numberOfUpdate = 1;
        int lengthOfPage = 1000;
        Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
        Page<Tenant> allTenants = tenantRepository.findAllTenants(page);

        while (!allTenants.isEmpty()) {
            page = page.next();

            updatePage(numberOfUpdate++, allTenants);

            allTenants = tenantRepository.findAllTenants(page);
        }
        log.info("Update for [" + allTenants.getTotalElements() + "] tenants finished");
    }


    private void updatePage(int numberOfUpdate, Page<Tenant> allTenants) {
        List<Tenant> tenantsToChange = new ArrayList<>();
        allTenants.forEach(tenant -> {
            tenant.setStatus(tenant.computeStatus());
            tenantsToChange.add(tenant);
        });

        tenantRepository.saveAll(tenantsToChange);
        log.info("Update number [" + numberOfUpdate + "] for " + allTenants.getNumberOfElements() + " tenants finished");
    }

    private int checkValueOfCustomMessage(CustomMessage customMessage) {

        List<MessageItem> messageItems = customMessage.getMessageItems();
        int allMessageResult = 0;

        if (!messageItems.isEmpty()) {
            for (MessageItem messageItem : messageItems) {
                ItemDetail messageItemCheck = messageItem.getItemDetailList().stream().filter(itemDetail ->
                        !messageItem.getCommentDoc().isEmpty() || itemDetail.isCheck()).findAny().orElse(null);
                if (messageItemCheck != null) {
                    forTenant = 2;
                    allMessageResult = forTenant;

                }
            }
        }

        List<GuarantorItem> guarantorItems = customMessage.getGuarantorItems();
        if (!guarantorItems.isEmpty()) {
            for (GuarantorItem guarantorItem : guarantorItems) {
                messageItems = guarantorItem.getMessageItems();
                for (MessageItem messageItem : messageItems) {
                    ItemDetail messageItemCheckGuarantor = messageItem.getItemDetailList().stream().filter(itemDetail ->
                            !messageItem.getCommentDoc().isEmpty() || itemDetail.isCheck()).findAny().orElse(null);
                    if (messageItemCheckGuarantor != null) {
                        allMessageResult = forTenant + 5;
                    }
                }
            }
        }

        return allMessageResult;
    }

    public String customMessage(Principal principal, Long tenantId, CustomMessage customMessage) {
        Tenant tenant = find(tenantId);
        if (tenant == null) {
            log.error("BOTenantController customEmail not found tenant with id : {}", tenantId);
            return "redirect:/error";
        }
        User operator = userService.findUserByEmail(principal.getName());
        updateFileStatus(customMessage);
        Message message = sendCustomMessage(tenant, customMessage, checkValueOfCustomMessage(customMessage));
        changeTenantStatusToDeclined(tenant, operator, message, ProcessedDocuments.NONE);

        return "redirect:/bo";
    }

    public void updateOperatorDateTimeTenant(Long tenantId) {
        Tenant tenant = find(tenantId);
        tenant.setOperatorDateTime(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    //todo : Review this method to refactor with the others DENY OR VALIDATE documents for tenants
    public String processFile(Long tenantId, CustomMessage customMessage, Principal principal) {
        Tenant tenant = find(tenantId);
        User operator = userService.findUserByEmail(principal.getName());

        if (tenant == null) {
            log.error("BOTenantController processFile not found tenant with id : {}", tenantId);
            return "redirect:/error";
        }

        ProcessedDocuments processedDocuments = ProcessedDocuments.in(customMessage);
        boolean allDocumentsValid = updateFileStatus(customMessage);

        if (allDocumentsValid) {
            changeTenantStatusToValidated(tenant, operator, processedDocuments);
        } else {
            Message message = sendCustomMessage(tenant, customMessage, checkValueOfCustomMessage(customMessage));
            changeTenantStatusToDeclined(tenant, operator, message, processedDocuments);
        }
        return "redirect:/bo";
    }

    //todo : Review this method to refactor with the others DENY OR VALIDATE documents for tenants
    public String updateStatusOfTenantFromAdmin(Principal principal, MessageDTO messageDTO, Long tenantId) {
        User operator = userService.findUserByEmail(principal.getName());
        Tenant tenant = tenantRepository.findOneById(tenantId);
        messageService.create(messageDTO, tenant, false, false);

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                        document.setDocumentStatus(DocumentStatus.DECLINED);
                        documentRepository.save(document);
                    }
                });
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(document -> {
                            if (document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                                document.setDocumentStatus(DocumentStatus.DECLINED);
                                documentRepository.save(document);
                            }
                        }));
        changeTenantStatusToDeclined(tenant, operator, null, ProcessedDocuments.ONE);

        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    public void updateTenantStatus(Tenant tenant, User operator) {
        TenantFileStatus previousStatus = tenant.getStatus();
        tenant.setStatus(tenant.computeStatus());
        tenantRepository.save(tenant);
        if (previousStatus != tenant.getStatus()) {
            switch (tenant.getStatus()) {
                case VALIDATED -> changeTenantStatusToValidated(tenant, operator, ProcessedDocuments.ONE);
                case DECLINED -> changeTenantStatusToDeclined(tenant, operator, null, ProcessedDocuments.ONE);
            }
        }
    }

    private void changeTenantStatusToValidated(Tenant tenant, User operator, ProcessedDocuments processedDocuments) {
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenantRepository.save(tenant);

        logService.saveByLog(new Log(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.getId()));
        operatorLogRepository.save(new OperatorLog(tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, processedDocuments.count()));

        if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.GROUP) {
            mailService.sendEmailToTenantAfterValidateAllDocumentsOfTenant(tenant);
        } else {
            if (tenant.getApartmentSharing().getTenants().stream()
                    .allMatch(t -> t.getStatus() == TenantFileStatus.VALIDATED)) {
                tenant.getApartmentSharing().getTenants().stream()
                        .filter(t -> StringUtils.isNotBlank(t.getEmail()))
                        .forEach(t -> mailService.sendEmailToTenantAfterValidateAllDocuments(t));
            }
        }

        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);

    }

    private void changeTenantStatusToDeclined(Tenant tenant, User operator, Message message, ProcessedDocuments processedDocuments) {
        tenant.setStatus(TenantFileStatus.DECLINED);
        tenantRepository.save(tenant);

        logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId(), (message == null) ? null : message.getId()));
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, processedDocuments.count()
        ));
        if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
            tenant.getApartmentSharing().getTenants().stream()
                    .filter(t -> StringUtils.isNotBlank(t.getEmail()))
                    .forEach(t -> mailService.sendEmailToTenantAfterTenantDenied(t, tenant));
        } else {
            mailService.sendMailNotificationAfterDeny(tenant);
        }
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
    }

    @Transactional
    public void updateStatusOfSomeTenants(String tenantList) {
        List<String> tenantList2 = Arrays.asList(tenantList.split(","));
        List<Long> idList = tenantList2.stream().map(Long::parseLong).collect(Collectors.toList());
        log.info("Found [" + idList.size() + "] tenants to recalculate their status");

        List<Tenant> tenantsById = tenantRepository.findAllById(idList);
        List<Tenant> tenantsToUpdate = new ArrayList<>();
        for (Tenant tenant : tenantsById) {
            TenantFileStatus newStatus = tenant.computeStatus();
            if (tenant.getStatus().ordinal() != newStatus.ordinal()) {
                log.info("Updating status of tenant with ID [" + tenant.getId() + "] from [" + tenant.getStatus() + "] to [" + newStatus.name() + "]");
                tenant.setStatus(newStatus);
                tenantsToUpdate.add(tenant);
            }
        }

        log.info("Tenants needed to update [" + tenantsToUpdate.size() + "]");
        tenantRepository.saveAll(tenantsToUpdate);
    }

    public List<Document> getAllDocumentCategories(Tenant tenant) {

        List<Document> documentList = tenant.getDocuments();

        List<DocumentCategory> listCategories = new ArrayList<>();
        listCategories.add(DocumentCategory.IDENTIFICATION);
        listCategories.add(DocumentCategory.RESIDENCY);
        listCategories.add(DocumentCategory.PROFESSIONAL);
        listCategories.add(DocumentCategory.FINANCIAL);
        listCategories.add(DocumentCategory.TAX);

        List<DocumentCategory> tenantListDocument = new ArrayList<>();
        documentList.forEach(document -> tenantListDocument.add(document.getDocumentCategory()));

        listCategories.forEach(documentCategory -> {
            if (!tenantListDocument.contains(documentCategory)) {
                Document docMissing = new Document();
                docMissing.setDocumentCategory(documentCategory);
                documentList.add(docMissing);
            }
        });

        documentList.sort(Comparator.comparing(Document::getDocumentCategory));
        return documentList;
    }

    public List<Document> getAllDocumentCategoriesGuarantor(Guarantor guarantor) {

        List<Document> documentList = guarantor.getDocuments();
        List<DocumentCategory> listCategories = new ArrayList<>();

        if (guarantor.getTypeGuarantor().name().equals("NATURAL_PERSON")) {

            listCategories.add(DocumentCategory.IDENTIFICATION);
            listCategories.add(DocumentCategory.RESIDENCY);
            listCategories.add(DocumentCategory.PROFESSIONAL);
            listCategories.add(DocumentCategory.FINANCIAL);
            listCategories.add(DocumentCategory.TAX);

            documentList = getMissingDocuments(guarantor, listCategories, documentList);
            return documentList;
        }

        if (guarantor.getTypeGuarantor().name().equals("LEGAL_PERSON")) {

            listCategories.add(DocumentCategory.IDENTIFICATION);
            listCategories.add(DocumentCategory.IDENTIFICATION_LEGAL_PERSON);
            documentList = getMissingDocuments(guarantor, listCategories, documentList);
            return documentList;

        }

        if (guarantor.getTypeGuarantor().name().equals("ORGANISM")) {
            if (guarantor.getDocuments().size() != 1) {
                Document docMissing = new Document();
                docMissing.setDocumentCategory(DocumentCategory.IDENTIFICATION);
                documentList.add(docMissing);
                return documentList;
            }
        }

        return documentList;
    }

    public List<Document> getMissingDocuments(Guarantor guarantor, List<DocumentCategory> listCategories, List<Document> documentList) {
        List<DocumentCategory> guarantorListDocument = new ArrayList<>();
        guarantor.getDocuments().forEach(document -> guarantorListDocument.add(document.getDocumentCategory()));
        listCategories.forEach(documentCategory -> {
            if (!guarantorListDocument.contains(documentCategory)) {
                Document docMissing = new Document();
                docMissing.setDocumentCategory(documentCategory);
                documentList.add(docMissing);
            }
        });
        documentList.sort(Comparator.comparing(Document::getDocumentCategory));
        return documentList;
    }

    @Transactional
    public void updateDocumentsWithNullCreationDateTime() {
        int numberOfUpdate = 1;
        int lengthOfPage = 1000;
        Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
        Page<Document> documents = documentRepository.findDocumentsByCreationDateTimeIsNull(page);
        long totalElements = documents.getTotalElements();
        log.info("[" + totalElements + "] documents to update with [creation_date=null]");
        while (!documents.isEmpty()) {
            page = page.next();

            updatePageOfDocumentsWithNullCreationDateTime(documents);
            log.info("Update number [" + numberOfUpdate++ + "] for " + documents.getNumberOfElements() + " documents finished");

            documents = documentRepository.findDocumentsByCreationDateTimeIsNull(page);
        }
        log.info("[" + totalElements + "] documents updated with [creation_date=null] finished");
    }

    private void updatePageOfDocumentsWithNullCreationDateTime(Page<Document> documents) {
        documents.forEach(document -> {
            if (document.getCreationDateTime() == null) {
                if (document.getTenant() != null) {
                    document.setCreationDateTime(document.getTenant().getLastUpdateDate());
                } else if (document.getGuarantor() != null) {
                    document.setCreationDateTime(document.getGuarantor().getTenant().getLastUpdateDate());
                }
                documentRepository.save(document);
            }
        });
    }

    public long getTotalOfTenantsWithFailedGeneratedPdfDocument() {
        return tenantRepository.countAllTenantsWithFailedGeneratedPdfDocument();
    }

    public Page<Tenant> getAllTenantsToProcessWithFailedGeneratedPdfDocument(Pageable pageable) {
        return new PageImpl<>(tenantRepository.findAllTenantsToProcessWithFailedGeneratedPdfDocument(pageable).toList());
    }

    public long countTenantsWithStatusInToProcess() {
        return tenantRepository.countAllByStatus(TenantFileStatus.TO_PROCESS);
    }

    public Tenant getTenantByEmail(String email) {
        return tenantRepository.findByEmail(email).get();
    }

    @Transactional
    public void regroupTenant(Tenant tenant, ApartmentSharing apartmentSharing, ApplicationType newApplicationType) {
        ApartmentSharing apartmentToDelete = tenant.getApartmentSharing();

        //Associating the tenant to the new apartment and disassociating the tenant from the current apartment
        apartmentSharing.getTenants().add(tenant);
        apartmentToDelete.getTenants().remove(tenant);

        if (apartmentSharing.getApplicationType() != ApplicationType.GROUP) {
            apartmentSharing.setApplicationType(newApplicationType);
        }
        apartmentSharingRepository.save(apartmentSharing);

        tenant.setTenantType(TenantType.JOIN);
        tenant.setApartmentSharing(apartmentSharing);
        tenantRepository.save(tenant);

        apartmentSharingRepository.delete(apartmentToDelete);
    }

    public Optional<Tenant> getOldestToProcessApplication() {
        Page<Tenant> page = tenantRepository.findToProcessApplicationsByOldestUpdateDate(PageRequest.of(0, 1));
        if (!page.isEmpty()) {
            return page.get().findFirst();
        }
        return Optional.empty();
    }

}

