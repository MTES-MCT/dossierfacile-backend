package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.CallbackLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.gouv.bo.dto.LightAPIInfoModel;
import fr.gouv.bo.model.apartment_sharing.ApplicationModel;
import fr.gouv.bo.repository.CallbackLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CallbackLogService {

    private final CallbackLogRepository callbackLogRepository;

    @SneakyThrows
    public void createCallbackLogForInternalPartnerLight(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, LightAPIInfoModel lightAPIInfoModel) {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(lightAPIInfoModel);
        callbackLogRepository.save(new CallbackLog(tenant.getId(), partnerId, tenantFileStatus, jsonContent));
    }

    @SneakyThrows
    public void createCallbackLogForPartnerModel(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, ApplicationModel applicationModel) {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(applicationModel);
        callbackLogRepository.save(new CallbackLog(tenant.getId(), partnerId, tenantFileStatus, jsonContent));
    }
}
