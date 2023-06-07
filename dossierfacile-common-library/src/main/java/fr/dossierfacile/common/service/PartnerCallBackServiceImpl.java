package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.LightAPIInfoModel;
import fr.dossierfacile.common.model.TenantLightAPIInfoModel;
import fr.dossierfacile.common.model.WebhookDTO;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.CallbackLogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.RequestService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCallBackServiceImpl implements PartnerCallBackService {
    private final TenantCommonRepository tenantRepository;
    private final TenantUserApiRepository tenantUserApiRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final RequestService requestService;
    private final CallbackLogService callbackLogService;
    private final ApartmentSharingRepository apartmentSharingRepository;

    @Value("${callback.domain:default}")
    private String callbackDomain;

    public void registerTenant(String internalPartnerId, Tenant tenant, UserApi userApi) {
        Optional<TenantUserApi> optionalTenantUserApi = tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApi);
        if (optionalTenantUserApi.isEmpty()) {
            TenantUserApi tenantUserApi = TenantUserApi.builder()
                    .id(new TenantUserApiKey(tenant.getId(), userApi.getId()))
                    .tenant(tenant)
                    .userApi(userApi)
                    .build();

            if (internalPartnerId != null && !internalPartnerId.isEmpty()) {
                if (tenantUserApi.getAllInternalPartnerId() == null) {
                    tenantUserApi.setAllInternalPartnerId(Collections.singletonList(internalPartnerId));
                } else if (!tenantUserApi.getAllInternalPartnerId().contains(internalPartnerId)) {
                    tenantUserApi.getAllInternalPartnerId().add(internalPartnerId);
                }
            }
            tenantUserApiRepository.save(tenantUserApi);

            if (userApi.getVersion() != null && userApi.getUrlCallback() != null && (
                    tenant.getStatus() == TenantFileStatus.VALIDATED
                            || tenant.getStatus() == TenantFileStatus.TO_PROCESS)) {
                PartnerCallBackType partnerCallBackType = tenant.getStatus() == TenantFileStatus.VALIDATED ?
                        PartnerCallBackType.VERIFIED_ACCOUNT :
                        PartnerCallBackType.CREATED_ACCOUNT;
                WebhookDTO webhookDTO = getWebhookDTO(tenant, userApi, partnerCallBackType);
                sendCallBack(tenant, webhookDTO);
            }
        }
    }

    public void sendCallBack(Tenant tenant, PartnerCallBackType partnerCallBackType) {
        Optional<ApartmentSharing> apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (apartmentSharing.isEmpty()) {
            return;
        }
        apartmentSharing.get().groupingAllTenantUserApisInTheApartment().forEach(tenantUserApi -> {
            UserApi userApi = tenantUserApi.getUserApi();
            WebhookDTO webhookDTO = getWebhookDTO(tenant, userApi, partnerCallBackType);
            sendCallBack(tenant, webhookDTO);
        });
    }

    @Override
    public void sendCallBack(List<Tenant> tenantList, PartnerCallBackType partnerCallBackType) {
        if (tenantList != null && !tenantList.isEmpty()) {
            tenantList.forEach(t -> sendCallBack(t, partnerCallBackType));
        }
    }

    public void sendCallBack(Tenant tenant, WebhookDTO webhookDTO) {
        if (webhookDTO == null) {
            log.error("WebhookDTO should not be null");
            return;
        }
        UserApi userApi = webhookDTO.getUserApi();
        if (userApi.isDisabled() || userApi.getUrlCallback() == null) {
            log.warn("UserApi call has not effect for " + userApi.getName());
            return;
        }
        if (userApi.getVersion() == null) {
            log.error("Unable to send callback to tenant " + tenant.getId() + " due to userApi version NULL");
            Sentry.captureMessage("userApi version is NULL for " + userApi.getName());
            return;
        }

        switch (userApi.getVersion()) {
            case 1 -> {
                if (webhookDTO instanceof LightAPIInfoModel) {
                    requestService.send((LightAPIInfoModel) webhookDTO, userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
                    callbackLogService.createCallbackLogForInternalPartnerLight(tenant, userApi.getId(), tenant.getStatus(), (LightAPIInfoModel) webhookDTO);
                }
            }
            case 2 -> {
                if (webhookDTO instanceof ApplicationModel) {
                    requestService.send((ApplicationModel) webhookDTO, userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
                    callbackLogService.createCallbackLogForPartnerModel(tenant, userApi.getId(), tenant.getStatus(), (ApplicationModel) webhookDTO);
                }
            }
            default -> log.error("send Callback failed");
        }
    }

    @Nullable
    @Override
    public WebhookDTO getWebhookDTO(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType) {
        WebhookDTO webhookDTO;
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId()).orElseThrow(() -> new NotFoundException("Apartment sharing not found"));
        switch (userApi.getVersion()) {
            case 1 -> webhookDTO = buildLightApiInfoModel(userApi, partnerCallBackType, apartmentSharing);
            case 2 -> webhookDTO = buildApplicationModelv2(tenant, userApi, partnerCallBackType, apartmentSharing);
            default -> {
                log.error("send Callback failed");
                webhookDTO = null;
            }
        }
        if (webhookDTO != null) {
            webhookDTO.setUserApi(userApi);
        }
        return webhookDTO;
    }

    @NotNull
    private ApplicationModel buildApplicationModelv2(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType, ApartmentSharing apartmentSharing) {
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModel(apartmentSharing, userApi);
        List<Tenant> tenantList = tenantRepository.findAllByApartmentSharing(apartmentSharing);
        for (Tenant t : tenantList) {
            tenantUserApiRepository.findFirstByTenantAndUserApi(t, userApi).ifPresent(tenantUserApi -> {
                if (tenantUserApi.getAllInternalPartnerId() != null && !tenantUserApi.getAllInternalPartnerId().isEmpty()) {
                    applicationModel.getTenants().stream()
                            .filter(tenantObject -> Objects.equals(tenantObject.getId(), t.getId()))
                            .findFirst()
                            .ifPresent(tenantModel -> tenantModel.setAllInternalPartnerId(tenantUserApi.getAllInternalPartnerId()));
                }
            });
        }
        applicationModel.setPartnerCallBackType(partnerCallBackType);
        applicationModel.setOnTenantId(tenant.getId());
        return applicationModel;
    }

    private LightAPIInfoModel buildLightApiInfoModel(UserApi userApi, PartnerCallBackType partnerCallBackType, ApartmentSharing apartmentSharing) {
        String fullAccessUrl = (partnerCallBackType == PartnerCallBackType.VERIFIED_ACCOUNT) ? (callbackDomain + "/file/" + apartmentSharing.getToken()) : "";
        String publicAccessUrl = (partnerCallBackType == PartnerCallBackType.VERIFIED_ACCOUNT) ? (callbackDomain + "/public-file/" + apartmentSharing.getTokenPublic()) : "";

        LightAPIInfoModel lightAPIInfoModel = LightAPIInfoModel.builder()
                .partnerCallBackType(partnerCallBackType)
                .url(fullAccessUrl)
                .publicUrl(publicAccessUrl)
                .emails(new ArrayList<>())
                .internalPartnersId(new ArrayList<>())
                .tenantLightAPIInfos(new ArrayList<>())
                .build();

        List<Tenant> tenantList = tenantRepository.findAllByApartmentSharing(apartmentSharing);
        for (Tenant t : tenantList) {
            if (t.getEmail() != null && !t.getEmail().isEmpty()) {
                lightAPIInfoModel.getEmails().add(t.getEmail());
            }

            TenantUserApi tenantUserApi = tenantUserApiRepository.findFirstByTenantAndUserApi(t, userApi).orElse(null);
            if (tenantUserApi != null && tenantUserApi.getAllInternalPartnerId() != null && !tenantUserApi.getAllInternalPartnerId().isEmpty()) {
                lightAPIInfoModel.getInternalPartnersId().addAll(tenantUserApi.getAllInternalPartnerId());
            }

            Map<String, String> hashMapFiles = new HashMap<>();

            List<Document> tenantDocuments = t.getDocuments();
            for (Document d : tenantDocuments) {
                hashMapFiles.put("tenantFile" + auxiliarDocumentCategory(d.getDocumentCategory()), d.getName());
            }

            List<Guarantor> guarantors = t.getGuarantors();
            boolean hasGuarantors = (guarantors != null && !guarantors.isEmpty());
            if (hasGuarantors) {
                for (Guarantor g : guarantors) {
                    List<Document> documents = g.getDocuments();
                    for (Document d : documents) {
                        String pathToDocument = d.getName();
                        hashMapFiles.put("guarantorFile" + auxiliarDocumentCategory(d.getDocumentCategory()), pathToDocument);
                    }
                }
            }

            lightAPIInfoModel.getTenantLightAPIInfos().add(
                    TenantLightAPIInfoModel.builder()
                            .email(t.getEmail())
                            .salary(t.getTotalSalary())
                            .tenantSituation(t.getTenantSituation().name())
                            .guarantor(hasGuarantors)
                            .listFiles(hashMapFiles)
                            .allInternalPartnerId(tenantUserApi != null ? tenantUserApi.getAllInternalPartnerId() : Collections.emptyList())
                            .build()
            );
        }
        return lightAPIInfoModel;
    }

    private int auxiliarDocumentCategory(DocumentCategory documentCategory) {
        return switch (documentCategory) {
            case IDENTIFICATION -> 1;
            case RESIDENCY -> 2;
            case PROFESSIONAL -> 3;
            case FINANCIAL -> 4;
            case TAX -> 5;
            default -> 0;
        };
    }
}
