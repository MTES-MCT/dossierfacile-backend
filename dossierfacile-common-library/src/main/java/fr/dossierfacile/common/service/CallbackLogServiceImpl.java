package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.common.entity.CallbackLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.LightAPIInfoModel;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.CallbackLogRepository;
import fr.dossierfacile.common.service.interfaces.CallbackLogService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CallbackLogServiceImpl implements CallbackLogService {

    private final CallbackLogRepository callbackLogRepository;

    @SneakyThrows
    @Override
    public void createCallbackLogForInternalPartnerLight(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, LightAPIInfoModel lightAPIInfoModel) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonContent = objectMapper.writeValueAsString(lightAPIInfoModel);
        callbackLogRepository.save(new CallbackLog(tenant.getId(),partnerId,tenantFileStatus, jsonContent));
    }

    @SneakyThrows
    @Override
    public void createCallbackLogForPartnerModel(Tenant tenant, Long partnerId, TenantFileStatus tenantFileStatus, ApplicationModel applicationModel) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonContent = objectMapper.writeValueAsString(applicationModel);
        callbackLogRepository.save(new CallbackLog(tenant.getId(),partnerId,tenantFileStatus, jsonContent));
    }
}
