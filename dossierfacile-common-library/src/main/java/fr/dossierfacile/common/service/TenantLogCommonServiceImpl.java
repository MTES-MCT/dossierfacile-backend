package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TenantLogCommonServiceImpl implements TenantLogCommonService {

    private final TenantLogRepository tenantLogRepository;

    @Override
    public void saveTenantLog(TenantLog log) {
        tenantLogRepository.save(log);
    }

}
