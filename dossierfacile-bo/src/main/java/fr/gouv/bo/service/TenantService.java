package fr.gouv.bo.service;

import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.mapper.mail.ApartmentSharingMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import fr.gouv.bo.dto.*;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.exception.GuarantorNotFoundException;
import fr.gouv.bo.model.ProcessedDocuments;
import fr.gouv.bo.repository.*;
import fr.gouv.bo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final Locale locale = LocaleContextHolder.getLocale();
    private final TenantCommonRepository tenantRepository;
    private final MailService mailService;
    private final PartnerCallBackService partnerCallBackService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final DocumentRepository documentRepository;
    private final DocumentDeniedReasonsRepository documentDeniedReasonsRepository;
    private final MessageService messageService;
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final OperatorLogRepository operatorLogRepository;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;
    private final DocumentService documentService;
    private final TenantLogService tenantLogService;
    private final KeycloakService keycloakService;
    private final ApartmentSharingService apartmentSharingService;
    private final GuarantorRepository guarantorRepository;
    private final TenantMapperForMail tenantMapperForMail;
    private final ApartmentSharingMapperForMail apartmentSharingMapperForMail;

    @Value("${time.reprocess.application.minutes}")
    private int timeReprocessApplicationMinutes;

    @Value("${process.max.dossier.time.interval:10}")
    private Long timeInterval;
    @Value("${process.max.dossier.by.interval:20}")
    private Long maxDossiersByInterval;
    @Value("${process.max.dossier.by.day:600}")
    private Long maxDossiersByDay;

    public Page<Tenant> getTenantByIdOrEmail(String email, Pageable pageable) {
        if (isNumeric(email)) {
            List<Tenant> result = Optional.ofNullable(tenantRepository.findOneById(Long.parseLong(email)))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            return new PageImpl<>(result, pageable, result.size());
        }
        if (email.contains("@")) {
            List<Tenant> result = tenantRepository.findByEmailIgnoreCase(email)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
            return new PageImpl<>(result, pageable, result.size());
        }
        return tenantRepository.findTenantByFirstNameOrLastNameOrFullName(email.toLowerCase(Locale.ROOT), pageable);
    }


    public Tenant findTenantById(Long id) {
        return tenantRepository.findOneById(id);
    }

    public User getUserById(Long id) {
        return tenantRepository.getReferenceById(id);
    }

    public Tenant getTenantById(Long id) {
        return tenantRepository.findOneById(id);
    }

    public Page<Tenant> listTenantsToProcess(Pageable pageable) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(timeReprocessApplicationMinutes);
        return tenantRepository.findTenantsToProcess(localDateTime, pageable);
    }

    public Tenant find(Long id) {
        return tenantRepository.findOneById(id);
    }

    public Tenant addOperatorComment(Long tenantId, String comment) {
        Tenant tenant = find(tenantId);
        tenant.setOperatorComment(comment);
        tenantLogService.addOperatorCommentLog(tenant, comment);
        return tenantRepository.save(tenant);
    }

    // ! used in thymeleaf template
    public Boolean hasVerifiedEmailIfExistsInKeycloak(Tenant tenant) {
        UserRepresentation keyCloakUser = keycloakService.getKeyCloakUser(tenant.getKeycloakId());
        if (keyCloakUser == null) {
            return null;
        }
        return keyCloakUser.isEmailVerified();
    }

    public synchronized String redirectToApplication(UserPrincipal operator, Long tenantId) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(timeReprocessApplicationMinutes);
        Tenant tenant;
        if (tenantId == null) {
            Long operatorId = operator.getId();
            // check less than x process are currently starting during the n lastMinutes
            if (operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(operatorId, ActionOperatorType.START_PROCESS, LocalDateTime.now().minusMinutes(timeInterval)) > maxDossiersByInterval) {
                throw new IllegalStateException("Vous ne pouvez pas ouvrir plus de " + maxDossiersByInterval + " dossiers pour traitement toutes les " + timeInterval + " minutes");
            }
            if (operatorLogRepository.countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(operatorId, ActionOperatorType.START_PROCESS, LocalDateTime.now().toLocalDate().atStartOfDay()) > maxDossiersByDay) {
                throw new IllegalStateException("Vous ne pouvez pas ouvrir plus de " + maxDossiersByDay + " dossiers par jour");
            }
            tenant = tenantRepository.findMyNextApplication(localDateTime, operatorId);
        } else {
            tenant = find(tenantId);
        }

        if (tenant != null) {
            User user = userService.findUserByEmail(operator.getName());
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
    public void validateTenantFile(UserPrincipal principal, Long tenantId) {
        Tenant tenant = find(tenantId);
        BOUser operator = userService.findUserByEmail(principal.getEmail());

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

    private void processDocumentDeniedReasons(MessageItem messageItem, List<Long> documentDeniedReasonsIds) {
        DocumentDeniedReasons documentDeniedReasons = new DocumentDeniedReasons();
        for (ItemDetail itemDetail : messageItem.getItemDetailList()) {
            if (itemDetail.isCheck()) {
                documentDeniedReasons.getCheckedOptions().add(itemDetail.getFormattedMessage());
                documentDeniedReasons.getCheckedOptionsId().add(itemDetail.getIdOptionMessage());
            }
        }

        if (!messageItem.getCommentDoc().isEmpty()) {
            documentDeniedReasons.setComment(messageItem.getCommentDoc());
        }

        if (!documentDeniedReasons.getCheckedOptionsId().isEmpty() || (documentDeniedReasons.getComment() != null && !documentDeniedReasons.getComment().isBlank())) {
            Document document = documentRepository.findById(messageItem.getDocumentId()).orElseThrow(() -> new DocumentNotFoundException(messageItem.getDocumentId()));
            documentDeniedReasons.setDocument(document);
            documentDeniedReasons.setDocumentCategory(document.getDocumentCategory());
            documentDeniedReasons.setDocumentSubCategory(document.getDocumentSubCategory());
            documentDeniedReasons.setDocumentCategoryStep(document.getDocumentCategoryStep());
            if (document.getGuarantor() != null) {
                documentDeniedReasons.setDocumentTenantType("guarantor");
            } else {
                documentDeniedReasons.setDocumentTenantType("tenant");
            }
            documentDeniedReasonsRepository.save(documentDeniedReasons);
            DocumentDeniedReasons documentDeniedReasonsToDelete = document.getDocumentDeniedReasons();
            documentService.updateDocumentWithDocumentDeniedReasons(documentDeniedReasons, messageItem.getDocumentId());
            if (documentDeniedReasonsToDelete != null) {
                documentDeniedReasonsRepository.delete(documentDeniedReasonsToDelete);
            }
            documentDeniedReasonsIds.add(documentDeniedReasons.getId());
        }
    }

    private void updateDocumentDeniedReasons(CustomMessage customMessage, Message message) {
        List<Long> documentDeniedReasonsIds = new ArrayList<>();
        for (MessageItem messageItem : customMessage.getMessageItems()) {
            processDocumentDeniedReasons(messageItem, documentDeniedReasonsIds);
        }
        for (GuarantorItem guarantorItem : customMessage.getGuarantorItems()) {
            for (MessageItem messageItem : guarantorItem.getMessageItems()) {
                processDocumentDeniedReasons(messageItem, documentDeniedReasonsIds);
            }
        }
        if (!documentDeniedReasonsIds.isEmpty()) {
            documentDeniedReasonsService.updateDocumentDeniedReasonsWithMessage(message, documentDeniedReasonsIds);
        }
    }

    private String documentCategoryLabel(MessageItem msg) {
        return messageSource.getMessage(msg.getDocumentCategory().getLabel(), null, locale);
    }

    private void appendDeniedReasons(StringBuilder html, String name, List<MessageItem> messageItems) {
        html.append("<li>");
        html.append("<strong class=\"name\">");
        html.append(name);
        html.append("</strong>");
        for (MessageItem messageItem : messageItems) {
            if (isDenied(messageItem)) {
                html.append("<hr/><strong>");
                html.append(documentCategoryLabel(messageItem));
                html.append("</strong>");
                html.append("<ul class=\"reasons\">");
                for (ItemDetail itemDetail : messageItem.getItemDetailList()) {
                    if (itemDetail.isCheck()) {
                        html.append("<li>");
                        html.append(itemDetail.getFormattedMessage());
                        html.append("</li>");
                    }
                }
                if (!messageItem.getCommentDoc().isEmpty()) {
                    html.append("<li>");
                    html.append(messageItem.getCommentDoc());
                    html.append("</li>");
                }
                html.append("</ul>");
            }
        }
        html.append("</li>");
    }

    private void appendCategoriesNames(StringBuilder html, List<MessageItem> messageItems) {
        for (MessageItem messageItem : messageItems) {
            if (isDenied(messageItem)) {
                html.append("<li class=\"category-name\">");
                html.append(documentCategoryLabel(messageItem));
                html.append("</li>");
            }
        }
    }

    private void appendCategoriesNamesForEmail(StringBuilder html, List<MessageItem> messageItems) {
        for (MessageItem messageItem : messageItems) {
            if (isDenied(messageItem)) {
                html.append("<li>");
                html.append(documentCategoryLabel(messageItem));
                html.append("</li>");
            }
        }
    }

    private void addHtmlSectionForEmail(StringBuilder builder, String name, List<MessageItem> messageItems) {
        builder.append("<div style=\"background-color:#f6f6f6;border-radius:4px;padding-top:10px;padding-left:10px;padding-right:10px;margin-bottom:10px;\">");
        builder.append("<strong>");
        builder.append(name);
        builder.append("</strong>");
        builder.append("<ul style=\"padding-bottom:10px;padding-top:10px;text-decoration:underline;line-height:30px;text-align: left\">");
        appendCategoriesNamesForEmail(builder, messageItems);
        builder.append("</ul>");
        builder.append("</div>");
    }

    private String guarantorLabel(GuarantorItem guarantorItem) {
        Long guarantorId = guarantorItem.getGuarantorId();
        Guarantor guarantor = guarantorRepository.findById(guarantorId).orElseThrow(() -> new GuarantorNotFoundException(guarantorId));
        return "Garant : " + guarantor.getCompleteName();
    }

    private void processMonthlySums(CustomMessage customMessage, Tenant tenant, Long operatorId) {
        List<Pair<String, List<MessageItem>>> itemsChanged = new ArrayList<>();
        var items = updateMonthlySums(customMessage.getMessageItems(), tenant, operatorId);
        if (items.size() > 0) {
            itemsChanged.add(Pair.of(tenant.getFullName(), items));
        }
        for (GuarantorItem guarantorItem : customMessage.getGuarantorItems()) {
            var guarantorItems = updateMonthlySums(guarantorItem.getMessageItems(), tenant, operatorId);
            if (guarantorItems.size() > 0) {
                itemsChanged.add(Pair.of(guarantorLabel(guarantorItem), guarantorItems));
            }
        }
        if (itemsChanged.size() > 0) {
            sendAmountChangedMessage(itemsChanged, tenant);
        }
    }

    private List<MessageItem> updateMonthlySums(List<MessageItem> items, Tenant tenant, Long operatorId) {
        List<MessageItem> itemsChanged = new ArrayList<>();
        for (MessageItem item : items) {
            if (item.getMonthlySum() != null && !item.getMonthlySum().equals(item.getNewMonthlySum())) {
                Document document = documentRepository.findById(item.getDocumentId()).orElse(null);
                if (document != null) {
                    log.info("Update document monthly sum : " + item.getDocumentId() + ", from " + item.getMonthlySum() + " to " + item.getNewMonthlySum());
                    tenantLogService.addUpdateAmountLog(tenant.getId(), operatorId, document, item.getNewMonthlySum());
                    document.setMonthlySum(item.getNewMonthlySum());
                    documentRepository.save(document);
                    itemsChanged.add(item);
                } else {
                    log.warn("Document not found: " + item.getDocumentId());
                }
            }
        }
        return itemsChanged;
    }

    public Message sendCustomMessage(Tenant tenant, CustomMessage customMessage) {
        boolean forTenant = hasCheckedItem(customMessage.getMessageItems());
        boolean forGuarantor = hasGuarantorCheckedItem(customMessage.getGuarantorItems());
        if (!forTenant && !forGuarantor) {
            return null;
        }

        List<MessageItem> messageItems = customMessage.getMessageItems();
        StringBuilder html = new StringBuilder();
        StringBuilder emailHtml = new StringBuilder();

        html.append("<ul class=\"custom-message\"><li><p>");
        html.append(messageSource.getMessage("bo.tenant.custom.email.head", null, locale));
        html.append("</p>");

        if (forTenant) {
            html.append("<strong class=\"name\">");
            html.append(tenant.getFirstName());
            html.append("</strong>");
            html.append("<ul class=\"doc-list\">");
            appendCategoriesNames(html, messageItems);
            html.append("</ul>");
        }

        List<GuarantorItem> guarantorItems = customMessage.getGuarantorItems();

        for (GuarantorItem guarantorItem : guarantorItems) {
            if (hasCheckedItem(guarantorItem.getMessageItems())) {
                html.append("<strong class=\"name\">");
                html.append(guarantorLabel(guarantorItem));
                html.append("</strong>");
                html.append("<ul class=\"doc-list\">");
                appendCategoriesNames(html, guarantorItem.getMessageItems());
                html.append("</ul>");
            }
        }
        html.append("</li>");

        if (forTenant) {
            appendDeniedReasons(html, tenant.getFirstName(), messageItems);
        }
        for (GuarantorItem guarantorItem : customMessage.getGuarantorItems()) {
            if (hasCheckedItem(guarantorItem.getMessageItems())) {
                appendDeniedReasons(html, guarantorLabel(guarantorItem), guarantorItem.getMessageItems());
            }
        }

        html.append("<li>");
        html.append(messageSource.getMessage("bo.tenant.custom.email.footer1", null, locale));
        html.append("</li></ul>");

        prepareEmailHtml(emailHtml, forTenant, tenant, messageItems, guarantorItems);

        Message message = messageService.create(new MessageDTO(html.toString(), emailHtml.toString()), tenant, false, true);
        updateDocumentDeniedReasons(customMessage, message);
        return message;
    }

    private void prepareEmailHtml(
            StringBuilder builder,
            boolean forTenant,
            Tenant tenant,
            List<MessageItem> messageItems,
            List<GuarantorItem> guarantorItems
    ) {
        if (forTenant) {
            addHtmlSectionForEmail(builder, tenant.getFirstName(), messageItems);
        }

        for (GuarantorItem guarantorItem : guarantorItems) {
            if (hasCheckedItem(guarantorItem.getMessageItems())) {
                addHtmlSectionForEmail(builder, guarantorLabel(guarantorItem), guarantorItem.getMessageItems());
            }
        }
    }

    @Transactional
    public void declineTenant(UserPrincipal principal, Long tenantId) {
        Tenant tenant = find(tenantId);
        User operator = userService.findUserByEmail(principal.getEmail());

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

    private boolean isDenied(MessageItem messageItem) {
        boolean messageItemCheck = messageItem.getItemDetailList().stream().anyMatch(ItemDetail::isCheck);
        return messageItemCheck || isNotEmpty(messageItem.getCommentDoc());
    }

    private boolean hasCheckedItem(List<MessageItem> messageItems) {
        return messageItems.stream().anyMatch(this::isDenied);
    }

    private boolean hasGuarantorCheckedItem(List<GuarantorItem> guarantorItems) {
        return guarantorItems.stream().anyMatch(item -> hasCheckedItem(item.getMessageItems()));
    }

    @Transactional
    public String customMessage(UserPrincipal principal, Long tenantId, CustomMessage customMessage) {
        Tenant tenant = find(tenantId);
        if (tenant == null) {
            log.error("BOTenantController customEmail not found tenant with id : {}", tenantId);
            return "redirect:/error";
        }
        User operator = userService.findUserByEmail(principal.getEmail());
        updateFileStatus(customMessage);
        Message message = sendCustomMessage(tenant, customMessage);
        changeTenantStatusToDeclined(tenant, operator, message, ProcessedDocuments.NONE);

        return "redirect:/bo";
    }

    public void updateOperatorDateTimeTenant(Long tenantId) {
        Tenant tenant = find(tenantId);
        tenant.setOperatorDateTime(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    @Transactional
    public void processFile(Long tenantId, CustomMessage customMessage, UserPrincipal principal) {
        Tenant tenant = find(tenantId);

        if (tenant == null) {
            log.error("Tenant not found from id : {}", tenantId);
            throw new IllegalStateException("You cannot treat an empty tenant");
        }
        //check tenant status before trying to validate or to deny
        if (tenant.getStatus() != TenantFileStatus.TO_PROCESS) {
            log.error("Operator try to validate/deny a not TO PROCESS tenant : t={} op={}", tenantId, principal.getEmail());
            throw new IllegalStateException("You cannot treat a tenant which is not TO PROCESS");
        }
        User operator = userService.findUserByEmail(principal.getEmail());

        ProcessedDocuments processedDocuments = ProcessedDocuments.in(customMessage);
        boolean allDocumentsValid = updateFileStatus(customMessage);

        processMonthlySums(customMessage, tenant, operator.getId());

        if (allDocumentsValid) {
            changeTenantStatusToValidated(tenant, operator, processedDocuments);
        } else {
            Message message = sendCustomMessage(tenant, customMessage);
            changeTenantStatusToDeclined(tenant, operator, message, processedDocuments);
        }
        updateOperatorDateTimeTenant(tenantId);
    }

    private void sendAmountChangedMessage(List<Pair<String, List<MessageItem>>> changeList, Tenant tenant) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Bonjour,</p>");
        if (changeList.size() == 1 && changeList.getFirst().getRight().size() == 1) {
            String name = changeList.getFirst().getLeft();
            MessageItem item = changeList.getFirst().getRight().getFirst();
            html.append("<p>Nos agents ont ajusté <strong>le montant de votre revenu</strong> déclaré afin qu’il corresponde à vos justificatifs.");
            html.append("<br/> Le montant suivant a été modifié pour garantir la cohérence et la fiabilité de votre dossier :");
            html.append("<p class=\"fr-mb-0\"><strong>");
            html.append(name);
            html.append("</strong></p>");
            html.append("<p><strong>");
            html.append(messageSource.getMessage("document_sub_category." + item.getDocumentSubCategory(), null, locale));
            if (item.getDocumentCategoryStep() != null) {
                html.append(" - ");
                html.append(messageSource.getMessage("document_category_step." + item.getDocumentCategoryStep(), null, locale));
            }
            html.append(" : </strong>");
            html.append(" déclaré <strong>");
            html.append(item.getMonthlySum());
            html.append(" €</strong> → corrigé à <strong>");
            html.append(item.getNewMonthlySum());
            html.append(" €</strong></p>");
        } else {
            html.append("<p>Nos agents ont ajusté <strong>certains montants de revenus</strong> déclarés afin qu’ils correspondent à vos justificatifs.");
            html.append("<br/> Les valeurs suivantes ont été modifiées pour garantir la cohérence et la fiabilité de votre dossier :");
            for (Pair<String,List<MessageItem>> change : changeList) {
                html.append("<p class=\"fr-mb-0\"><strong>");
                html.append(change.getLeft());
                html.append("</strong></p>");
                html.append("<ul>");
                for (MessageItem item: change.getRight()) {
                    html.append("<li>");
                    html.append("<strong>");
                    html.append(messageSource.getMessage("document_sub_category." + item.getDocumentSubCategory(), null, locale));
                    if (item.getDocumentCategoryStep() != null) {
                        html.append(" - ");
                        html.append(messageSource.getMessage("document_category_step." + item.getDocumentCategoryStep(), null, locale));
                    }
                    html.append(" : </strong>");
                    html.append(" déclaré <strong>");
                    html.append(item.getMonthlySum());
                    html.append(" €</strong> → corrigé à <strong>");
                    html.append(item.getNewMonthlySum());
                    html.append(" €</strong>");
                    html.append("</li>");
                }
                html.append("</ul>");
            }
        }
        html.append("<p>👉 Vous pouvez consulter la version mise à jour dans votre espace.</p>");
        html.append("<p>Si vous souhaitez modifier ce montant, vous êtes libre de le faire, mais votre dossier devra alors repasser par le processus complet de validation.<br/> ");
        html.append("<strong>Pour un traitement plus rapide, nous vous invitons à contacter notre support via ce lien : <a href=\"/contact?open=form\">Lien support</a>.</strong></p>");
        html.append("<p><em>Rappel : vous avez accepté que notre équipe procède à cet ajustement en cas d’incohérence.</em></p>");
        html.append("<p>Bonne journée</p>");
        messageService.create(MessageDTO.builder().message(html.toString()).build(), tenant, false, false);
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        mailService.sendEmailAmountChanged(tenantDto);
    }

    @Transactional
    //todo : Review this method to refactor with the others DENY OR VALIDATE documents for tenants
    public String updateStatusOfTenantFromAdmin(
            UserPrincipal principal,
            MessageDTO messageDTO,
            Long tenantId
    ) {
        User operator = userService.findUserByEmail(principal.getEmail());
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


    @Transactional
    private void updateTenantStatus(Tenant tenant, User operator) {
        TenantFileStatus previousStatus = tenant.getStatus();
        tenant.setStatus(tenant.computeStatus());
        tenantRepository.save(tenant);
        if (previousStatus != tenant.getStatus()) {
            switch (tenant.getStatus()) {
                case VALIDATED -> changeTenantStatusToValidated(tenant, operator, ProcessedDocuments.ONE);
                case DECLINED -> changeTenantStatusToDeclined(tenant, operator, null, ProcessedDocuments.ONE);
            }
            messageService.markReadAdmin(tenant);
        }
    }


    private void changeTenantStatusToValidated(Tenant tenant, User operator, ProcessedDocuments processedDocuments) {
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenantRepository.save(tenant);

        // TODO: Remove after sharing page is implemented
        boolean hasLinks = tenant.getApartmentSharing().getApartmentSharingLinks().stream()
            .anyMatch(link -> link.getLinkType() == ApartmentSharingLinkType.LINK && link.getCreatedBy() == tenant.getId());
        if (!hasLinks) {
            ApartmentSharingLink link = buildApartmentSharingLink(tenant.getApartmentSharing(), tenant.getId(), false);
            ApartmentSharingLink linkFull = buildApartmentSharingLink(tenant.getApartmentSharing(), tenant.getId(), true);
            apartmentSharingLinkRepository.save(link);
            apartmentSharingLinkRepository.save(linkFull);
        }

        messageService.markReadAdmin(tenant);

        tenantLogService.saveByLog(new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.getId()));
        operatorLogRepository.save(new OperatorLog(tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, processedDocuments.count(), processedDocuments.timeSpent()));

        // prepare for mail
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        ApartmentSharingDto apartmentSharingDto = apartmentSharingMapperForMail.toDto(tenant.getApartmentSharing());

        // sendCallBack is sent after Commit
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);

        TransactionalUtil.afterCommit(() -> {
            try {
                if (apartmentSharingDto.getTenants().stream().allMatch(t -> t.getStatus() == TenantFileStatus.VALIDATED)) {
                    apartmentSharingDto.getTenants().stream()
                            .filter(t -> isNotBlank(t.getEmail()))
                            .forEach(t -> {
                                if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.GROUP) {
                                    mailService.sendEmailToTenantAfterValidateAllTenantForGroup(t);
                                } else {
                                    mailService.sendEmailToTenantAfterValidateAllDocuments(t);
                                }
                            });
                } else if (apartmentSharingDto.getApplicationType() == ApplicationType.GROUP) {
                    mailService.sendEmailToTenantAfterValidatedApartmentSharingNotValidated(tenantDto);
                }
            } catch (Exception e) {
                log.error("CAUTION Unable to send notification to user ", e);
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



    private void changeTenantStatusToDeclined(Tenant tenant, User operator, Message message, ProcessedDocuments processedDocuments) {
        tenant.setStatus(TenantFileStatus.DECLINED);
        tenantRepository.save(tenant);
        messageService.markReadAdmin(tenant);

        tenantLogService.saveByLog(new TenantLog(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId(), (message == null) ? null : message.getId()));
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, processedDocuments.count(), processedDocuments.timeSpent()
        ));

        // prepare for mail
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        ApartmentSharingDto apartmentSharingDto = apartmentSharingMapperForMail.toDto(tenant.getApartmentSharing());

        // sendCallBack is sent after Commit
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);

        TransactionalUtil.afterCommit(() -> {
            if (apartmentSharingDto.getApplicationType() == ApplicationType.COUPLE) {
                apartmentSharingDto.getTenants().stream()
                        .filter(t -> isNotBlank(t.getEmail()))
                        .forEach(t -> mailService.sendEmailToTenantAfterTenantDenied(t, tenantDto, message));
            } else {
                mailService.sendMailNotificationAfterDeny(tenantDto, message);
            }

        });
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

    public long getCountOfTenantsWithFailedGeneratedPdfDocument() {
        return tenantRepository.countAllTenantsWithoutPdfDocument();
    }

    public Page<Tenant> getAllTenantsToProcessWithFailedGeneratedPdfDocument(Pageable pageable) {
        return tenantRepository.findAllTenantsToProcessWithoutPdfDocument(pageable);
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

    @Transactional
    public Tenant deleteDocument(Long id, User operator) {
        Document document = documentService.findDocumentById(id);
        Tenant tenant = documentService.deleteDocument(id);
        tenantLogService.addDeleteDocumentLog(tenant.getId(), operator.getId(), document);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        updateTenantStatus(tenant, operator);
        return tenant;
    }

    @Transactional
    public Tenant changeDocumentStatus(Long id, MessageDTO messageDTO, User operator) {
        Tenant tenant = documentService.changeStatusOfDocument(id, messageDTO);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        updateTenantStatus(tenant, operator);
        return tenant;
    }

    @Transactional
    public Tenant deleteGuarantor(Long guarantorId, User operator) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId).orElseThrow(() -> new GuarantorNotFoundException(guarantorId));
        Tenant tenant = guarantor.getTenant();
        tenant.getGuarantors().remove(guarantor);
        guarantorRepository.deleteById(guarantorId);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());

        updateTenantStatus(tenant, operator);
        return tenant;
    }
}

