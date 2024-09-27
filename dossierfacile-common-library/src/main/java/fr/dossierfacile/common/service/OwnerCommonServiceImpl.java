package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.OwnerCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.OwnerCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class OwnerCommonServiceImpl implements OwnerCommonService {
    private final TenantCommonRepository tenantCommonRepository;

    private final OwnerCommonRepository ownerCommonRepository;
    private ApartmentSharingCommonService apartmentSharingCommonService;

    @Override
    public Owner findByKeycloakId(String keycloakId) {
        return ownerCommonRepository.findByKeycloakId(keycloakId);
    }
}
