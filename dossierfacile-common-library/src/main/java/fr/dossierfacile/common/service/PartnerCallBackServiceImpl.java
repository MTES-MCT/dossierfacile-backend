package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.WebhookDTO;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.CallbackLogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
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
        log.info("Send Callback for " + tenant.getId() + " to" + webhookDTO.getUserApi().getName());
        UserApi userApi = webhookDTO.getUserApi();
        if (userApi.isDisabled() || StringUtils.isBlank(userApi.getUrlCallback())) {
            log.warn("UserApi call has not effect for " + userApi.getName());
            return;
        }
        if (userApi.getVersion() != 2) {
            log.error("Unable to send callback to tenant " + tenant.getId() + " due to userApi version" + userApi.getVersion());
            return;
        }
        requestService.send((ApplicationModel) webhookDTO, userApi.getUrlCallback(), userApi.getPartnerApiKeyCallback());
        callbackLogService.createCallbackLogForPartnerModel(tenant, userApi.getId(), tenant.getStatus(), (ApplicationModel) webhookDTO);

    }

    @Override
    public void sendRevokedAccessCallback(Tenant tenant, UserApi userApi) {
        ApplicationModel webhook = new ApplicationModel();
        webhook.setOnTenantId(tenant.getId());
        webhook.setPartnerCallBackType(PartnerCallBackType.ACCESS_REVOKED);
        webhook.setUserApi(userApi);

        sendCallBack(tenant, webhook);
    }

    @NotNull
    public ApplicationModel getWebhookDTO(Tenant tenant, UserApi userApi, PartnerCallBackType partnerCallBackType) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModel(apartmentSharing, userApi);
        applicationModel.setUserApi(userApi);
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
}
