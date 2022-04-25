package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
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
import fr.gouv.bo.repository.ApartmentSharingRepository;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.repository.UserApiRepository;
import fr.gouv.bo.utils.UtilsLocatio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final String USER_TYPE_TENANT = "tenant";
    private static final String USER_TYPE_GUARANTOR = "guarantor";
    private static final String LI_P = "<li><p>";
    private static final String P_LI = "</p></li>";
    private static final String BOLD_CLOSE = "</b> ";
    private static final String IDENTITY_KEY = "documentation";
    private static final String IDENTITY_REPLACE = "<a style=\"color: black;font-weight: bolder\" rel=\"nofollow\" href=\"https://docs.dossierfacile.fr/guide-dutilisation-de-dossierfacile/ajouter-un.e-conjoint.e\">documentation</a>";
    private static final String IDENTITY_REPLACE_DB = "<a  class=\"bold\" style=\"color: black;\" rel=\"nofollow\" href=\"https://docs.dossierfacile.fr/guide-dutilisation-de-dossierfacile/ajouter-un.e-conjoint.e\">documentation</a>";
    private static final String TENANT_KEY = "pour les trois derniers mois";
    private static final String TENANT_REPLACE = "<mark style=\"font-weight: bolder; background:none\">pour les trois derniers mois</mark>";
    private static final String TENANT_REPLACE_DB = "<mark class=\"bold\" style=\"background: #dedfdd\">pour les trois derniers mois</mark>";
    private static final String SOCIAL_KEY = "vos trois derniers justificatifs";
    private static final String SOCIAL_REPLACE = "<mark style=\"font-weight: bolder; background:none\">vos trois derniers justificatifs</mark>";
    private static final String SOCIAL_REPLACE_DB = "<mark class=\"bold\" style=\"background: #dedfdd\">vos trois derniers justificatifs</mark>";
    private static final String SALARY_KEY = "des 3 derniers mois";
    private static final String SALARY_REPLACE = "<mark style=\"font-weight: bolder; background:none\">des 3 derniers mois</mark>";
    private static final String SALARY_REPLACE_DB = "<mark class=\"bold\" style=\"background: #dedfdd\">des 3 derniers mois</mark>";

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
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final OperatorLogRepository operatorLogRepository;
    private final LogActionTenantStatusService logService;
    private final DocumentDeniedReasonsService documentDeniedReasonsService;
    private final DocumentService documentService;
    private final DocumentDeniedOptionsRepository documentDeniedOptionsRepository;

    private int forTenant = 0;
    @Value("${time.reprocess.application.minutes}")
    private int timeReprocessApplicationMinutes;

    public List<Tenant> getTenantByIdOrEmail(EmailDTO emailDTO) {
        List<Tenant> tenantList = new ArrayList<>();
        if (UtilsLocatio.isNumeric(emailDTO.getEmail())) {
            tenantList.add(tenantRepository.findOneById(Long.parseLong(emailDTO.getEmail())));
            return tenantList;
        }
        if (emailDTO.getEmail().contains("@")) {
            tenantList.add(tenantRepository.findOneByEmail(emailDTO.getEmail()));
            tenantList.add(new Tenant());
            return tenantList;
        }
        return searchTenantByName(emailDTO);
    }

    public List<Tenant> searchTenantByName(EmailDTO emailDTO) {
        return tenantRepository.findTenantByFirstNameOrLastNameOrFullName(emailDTO.getEmail().toLowerCase(Locale.ROOT));
    }

    public Tenant findTenantCreate(Long id) {
        return tenantRepository.findAllByApartmentSharingId(id).stream().filter(c -> c.getTenantType().equals(TenantType.CREATE)).findFirst().orElse(null);
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

    public synchronized String redirectToApplication(Principal principal, Long tenantId) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(timeReprocessApplicationMinutes);
        Tenant tenant;
        if (tenantId == null) {
            tenant = tenantRepository.findNextApplication(localDateTime);
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

    public Long countUploadedFiles() {
        return tenantRepository.countTotalUploadedFiles();
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
        User operator = userService.findUserByEmail(principal.getName());

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
        changeTenantStatusToValidated(tenant);

        mailService.sendEmailToTenantAfterValidateAllDocuments(tenant);
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS
        ));
        logService.saveByLog(new Log(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.getId()));
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

        createStylesInMessages(customMessage);
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
                        mailMessage.append(itemDetail.getMessage());
                        mailMessage.append(P_LI);
                        DocumentDeniedOptions documentDeniedOptions = setCheckedOptionsId(messageItem.getDocumentId(), USER_TYPE_TENANT, itemDetail.getMessage());
                        documentDeniedReasons.getCheckedOptionsId().add(documentDeniedOptions.getId());
                        documentDeniedReasons.getCheckedOptions().add(itemDetail.getMessage());
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
                    documentDeniedReasonsRepository.save(documentDeniedReasons);
                    Document document = documentRepository.findById(messageItem.getDocumentId()).orElseThrow(() -> new DocumentNotFoundException(messageItem.getDocumentId()));
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
                            mailMessage.append(itemDetail.getMessage());
                            mailMessage.append(P_LI);
                            DocumentDeniedOptions documentDeniedOptions = setCheckedOptionsId(messageItem.getDocumentId(), USER_TYPE_GUARANTOR, itemDetail.getMessage());
                            documentDeniedReasons.getCheckedOptionsId().add(documentDeniedOptions.getId());
                            documentDeniedReasons.getCheckedOptions().add(itemDetail.getMessage());
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
                        documentDeniedReasonsRepository.save(documentDeniedReasons);
                        Document document = documentRepository.findById(messageItem.getDocumentId()).orElseThrow(() -> new DocumentNotFoundException(messageItem.getDocumentId()));
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

    private DocumentDeniedOptions setCheckedOptionsId(Long documentId, String userType, String message) {
        Document document = documentRepository.findById(documentId).orElse(null);
        assert document != null;
        if (userType.equals(USER_TYPE_TENANT)) {
            if (message.contains(IDENTITY_REPLACE)) {
                message = message.replace(IDENTITY_REPLACE, IDENTITY_REPLACE_DB);
            }
            if (message.contains(SOCIAL_REPLACE)) {
                message = message.replace(SOCIAL_REPLACE, SOCIAL_REPLACE_DB);
            }
            if (message.contains(TENANT_REPLACE)) {
                message = message.replace(TENANT_REPLACE, TENANT_REPLACE_DB);
            }
            if (message.contains(SALARY_REPLACE)) {
                message = message.replace(SALARY_REPLACE, SALARY_REPLACE_DB);
            }
        }

        return documentDeniedOptionsRepository.findOneDocumentDeniedOptions(document.getDocumentSubCategory().toString(), userType, message);
    }

    public void createStylesInMessages(CustomMessage customMessage) {

        customMessage.getMessageItems().forEach(messageItem -> messageItem.getItemDetailList().forEach(itemDetail -> {
            if (messageItem.getDocumentCategory().equals(DocumentCategory.IDENTIFICATION) && itemDetail.getMessage().contains(IDENTITY_KEY)) {
                itemDetail.setMessage(itemDetail.getMessage().replace(IDENTITY_KEY, IDENTITY_REPLACE));
            }
            if (messageItem.getDocumentSubCategory().equals(DocumentSubCategory.SOCIAL_SERVICE) && itemDetail.getMessage().contains(SOCIAL_KEY)) {
                itemDetail.setMessage(itemDetail.getMessage().replace(SOCIAL_KEY, SOCIAL_REPLACE));
            }
            if (messageItem.getDocumentSubCategory().equals(DocumentSubCategory.TENANT) && itemDetail.getMessage().contains(TENANT_KEY)) {
                itemDetail.setMessage(itemDetail.getMessage().replace(TENANT_KEY, TENANT_REPLACE));
            }
            if (messageItem.getDocumentSubCategory().equals(DocumentSubCategory.SALARY) && itemDetail.getMessage().contains(SALARY_KEY)) {
                itemDetail.setMessage(itemDetail.getMessage().replace(SALARY_KEY, SALARY_REPLACE));
            }
        }));
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
        changeTenantStatusToDeclined(tenant);

        mailService.sendMailNotificationAfterDeny(tenant);
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS
        ));
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
        logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId()));
    }

    public void sendCallBacksManuallyToUserApi(Long userApiId) {
        synchronized (this) {
            UserApi userApi = userApiRepository.findOneById(userApiId);

            //Finding the IDs of tenants pending to send info for partner with ID 2.
            List<Long> ids = tenantRepository.listIdTenantsAccountCompletedPendingToSendCallBack(userApiId);
            int numberOfTotalCalls = ids.size();
            log.info(numberOfTotalCalls + " tenants pending to send the validation information to the partner.");
            int indexCall = 1;
            for (Long id : ids) {
                Tenant tenant = tenantRepository.findOneById(id);
                partnerCallBackService.sendCallBack(tenant, userApi, PartnerCallBackType.VERIFIED_ACCOUNT);

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

    public void callBackManuallyToLocserviceIds() {
        synchronized (this) {
            UserApi userApi = userApiRepository.findOneById(2L);

            List<Integer> ints = Arrays.asList(5701, 5537, 5383, 5290, 5033, 4998, 4922, 4895, 4890, 4668, 4029, 3133, 3120, 3107, 3101, 3093, 3064, 3061, 2968, 2965, 2963, 2946, 2873, 2867, 2848, 2818, 2813, 2808, 2804, 2786, 2784, 2772, 2765, 2756, 2741, 2740, 2723, 2711, 2702, 2692, 2685, 2678, 2674, 2655, 2645, 2640, 2636, 2624, 2608, 2580, 2563, 2539, 2521, 2507, 2490, 2480, 2478, 2472, 2451, 2435, 2387, 2357, 2314, 2282, 2275, 2259, 2258, 2251, 2248, 2242, 2235, 2234, 2217, 2154, 2122, 2089, 2086, 2082, 2067, 2066, 2056, 2049, 2046, 1999, 1983, 1974, 1937, 1936, 1919, 1916, 1829, 1815, 1793, 1777, 1767, 1761, 1742, 1722, 1720, 1699, 1645, 1633, 1632, 1625, 1618, 1612, 1549, 1526, 1494, 1466, 1461, 1441, 1403, 1384, 1379, 1378, 1373, 1359, 1353, 1286, 1270, 1262, 1260, 1259, 1257, 1256, 1254, 1225, 1219, 1198, 1172, 1136, 1123, 1062, 1046, 1032, 1008, 1006, 960, 950, 939, 933, 930, 891, 864, 833, 814, 750, 742, 702, 698, 684, 675, 665, 656, 651, 643, 642, 631, 560, 554, 545, 538, 521, 475, 472, 462, 429, 417, 374, 358, 352, 347, 321, 320, 319, 275, 202, 186, 165, 148, 147, 146, 108);
            List<Long> longs = ints.stream()
                    .mapToLong(Integer::longValue)
                    .boxed().collect(Collectors.toList());
            int numberOfTotalCalls = longs.size();
            log.info(numberOfTotalCalls + " tenants pending to send the validation information to the partner.");
            int indexCall = 1;
            for (Long id : longs) {
                Tenant tenant = tenantRepository.findOneById(id);
                partnerCallBackService.sendCallBack(tenant, userApi, PartnerCallBackType.VERIFIED_ACCOUNT);

                if (indexCall < numberOfTotalCalls) {
                    try {
                        indexCall++;
                        log.info("Waiting 50ms for the next call...");

                        this.wait(50);
                    } catch (InterruptedException e) {
                        log.error("InterruptedException callBackManually ", e);
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
        updateFileStatus(customMessage);
        changeTenantStatusToDeclined(tenant);
        Message message = sendCustomMessage(tenant, customMessage, checkValueOfCustomMessage(customMessage));
        mailService.sendMailNotificationAfterDeny(tenant);

        User operator = userService.findUserByEmail(principal.getName());
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS
        ));
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
        if (message != null) {
            logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId(), message.getId()));
        } else {
            logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId()));
        }
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
        boolean allDocumentsValid = updateFileStatus(customMessage);
        Message message = sendCustomMessage(tenant, customMessage, checkValueOfCustomMessage(customMessage));
        if (allDocumentsValid) {
            changeTenantStatusToValidated(tenant);
            logService.saveByLog(new Log(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.getId()));
            mailService.sendEmailToTenantAfterValidateAllDocuments(tenant);
        } else {
            changeTenantStatusToDeclined(tenant);
            if (message != null) {
                logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId(), message.getId()));
            } else {
                logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId()));
            }
            mailService.sendMailNotificationAfterDeny(tenant);
        }
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS
        ));
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
        changeTenantStatusToDeclined(tenant);
        mailService.sendMailNotificationAfterDeny(tenant);
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS
        ));
        logService.saveByLog(new Log(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId()));
        return "redirect:/bo/colocation/" + tenant.getApartmentSharing().getId() + "#tenant" + tenant.getId();
    }

    public void updateTenantStatus(Tenant tenant) {
        TenantFileStatus previousStatus = tenant.getStatus();
        tenant.setStatus(tenant.computeStatus());
        tenantRepository.save(tenant);
        if (previousStatus != tenant.getStatus()) {
            switch (tenant.getStatus()) {
                case VALIDATED: {
                    partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);
                    break;
                }
                case DECLINED: {
                    partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);
                    break;
                }
            }
        }
    }

    public void changeTenantStatusToValidated(Tenant tenant) {
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenantRepository.save(tenant);
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.VERIFIED_ACCOUNT);
    }

    public void changeTenantStatusToDeclined(Tenant tenant) {
        tenant.setStatus(TenantFileStatus.DECLINED);
        tenantRepository.save(tenant);
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
    public void deleteAccountsNotProperlyDeleted() {
        int numberOfDeletionPage = 1;
        int lengthOfPage = 100;
        Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
        Page<ApartmentSharing> apartmentSharingsToDelete = apartmentSharingRepository.findAllByUserIdInAccountDeleteLog(page);
        long totalToDelete = apartmentSharingsToDelete.getTotalElements();

        while (!apartmentSharingsToDelete.isEmpty()) {
            page = page.next();

            deletePageOfApartmentSharing(numberOfDeletionPage++, apartmentSharingsToDelete);

            apartmentSharingsToDelete = apartmentSharingRepository.findAllByUserIdInAccountDeleteLog(page);
        }
        log.info("Deletion for [" + totalToDelete + "] apartments sharing finished");
    }


    private void deletePageOfApartmentSharing(int numberOfDeletionPage, Page<ApartmentSharing> apartmentSharingPage) {
        apartmentSharingRepository.deleteAll(apartmentSharingPage);
        log.info("Deletion page number [" + numberOfDeletionPage + "] for " + apartmentSharingPage.getNumberOfElements() + " apartment_sharing finished");
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

    public Page<Tenant> getAllTenantsWithFailedGeneratedPdfDocument(Pageable pageable) {
        return new PageImpl<>(tenantRepository.findAllTenantsWithFailedGeneratedPdfDocument(pageable).toList());
    }

    public long getTenantsWithStatusInToProcess() {
        return tenantRepository.getTenantsByStatus(TenantFileStatus.TO_PROCESS).size();
    }
}

