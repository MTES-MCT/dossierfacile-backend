package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.gouv.bo.dto.LightAPIInfoModel;
import fr.gouv.bo.dto.TenantLightAPIInfoModel;
import fr.gouv.bo.mapper.ApplicationFullMapper;
import fr.gouv.bo.repository.TenantRepository;
import fr.gouv.bo.repository.TenantUserApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartnerCallBackService {

    private final TenantRepository tenantRepository;
    private final TenantUserApiRepository tenantUserApiRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final RequestService requestService;
    private final CallbackLogService callbackLogService;

    @Value("${callback.domain}")
    private String callbackDomain;

    public void sendCallBack(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();

        switch (userApi.getVersion()) {
            case 1: {
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

                    TenantUserApi tenantUserApi = tenantUserApiRepository.findFirstByTenantAndUserApi(t, userApi);
                    if (tenantUserApi != null && tenantUserApi.getAllInternalPartnerId() != null && !tenantUserApi.getAllInternalPartnerId().isEmpty()) {
                        lightAPIInfoModel.getInternalPartnersId().addAll(tenantUserApi.getAllInternalPartnerId());
                    }

                    Map<String, String> hashMapFiles = new HashMap<>();

                    List<Document> tenantDocuments = t.getDocuments();
                    for (Document d : tenantDocuments) {
                        hashMapFiles.put("tenantFile" + auxiliarDocumentCategory(d.getDocumentCategory()), d.getName());
                    }

                    List<Guarantor> guarantors = t.getGuarantors();
                    boolean hasGuarantors;
                    hasGuarantors = (guarantors != null && !guarantors.isEmpty());
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

                requestService.send(lightAPIInfoModel, userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
                callbackLogService.createCallbackLogForInternalPartnerLight(tenant, userApi.getId(), tenant.getStatus(), lightAPIInfoModel);
                break;
            }
            case 2: {
                requestService.send(applicationFullMapper.toApplicationModel(apartmentSharing), userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
                callbackLogService.createCallbackLogForPartnerModel(tenant, userApi.getId(), tenant.getStatus(), applicationFullMapper.toApplicationModel(apartmentSharing));
                break;
            }
        }
    }

    private int auxiliarDocumentCategory(DocumentCategory documentCategory) {
        switch (documentCategory) {
            case IDENTIFICATION:
                return 1;
            case RESIDENCY:
                return 2;
            case PROFESSIONAL:
                return 3;
            case FINANCIAL:
                return 4;
            case TAX:
                return 5;
            default:
                return 0;
        }
    }
}
