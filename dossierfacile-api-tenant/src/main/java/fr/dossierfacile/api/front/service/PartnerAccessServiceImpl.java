package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.mapper.PartnerAccessMapper;
import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.api.front.service.interfaces.PartnerAccessService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.dossierfacile.common.constants.PartnerConstants.DF_OWNER_NAME;

@Slf4j
@Service
@AllArgsConstructor
public class PartnerAccessServiceImpl implements PartnerAccessService {

    private final PartnerAccessMapper partnerAccessMapper;

    @Override
    public List<PartnerAccessModel> getExternalPartners(Tenant tenant) {
        return tenant.getTenantsUserApi().stream()
                .filter(api -> !DF_OWNER_NAME.equals(api.getUserApi().getName()))
                .map(partnerAccessMapper::toModel)
                .toList();
    }




}
