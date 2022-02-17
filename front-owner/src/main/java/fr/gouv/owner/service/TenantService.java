package fr.gouv.owner.service;


import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.owner.dto.CountDTO;
import fr.gouv.owner.dto.TenantDTO;
import fr.gouv.owner.model.KeyStatistics;
import fr.gouv.owner.repository.TenantRepository;
import fr.gouv.owner.utils.UtilsLocatio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public List<Tenant> getAllTenantByApartmentSharing(PropertyApartmentSharing propertyApartmentSharing) {
        return tenantRepository.findTenantByApartmentSharingId(propertyApartmentSharing.getApartmentSharing().getId());
    }

    public Tenant find(Long id) {
        return tenantRepository.findOneById(id);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public Map<KeyStatistics, Map<String, Long>> acountCreationStatistics() {
        Map<KeyStatistics, Map<String, Long>> map = new HashMap<>();
        List<CountDTO> count = tenantRepository.countAllRegisteredTenant();
        UtilsLocatio.extractStatistics(map, count, "creation");
        return map;
    }

    public Map<KeyStatistics, Map<String, Long>> statistics() {
        Map<KeyStatistics, Map<String, Long>> map = new HashMap<>();
        List<CountDTO> count = tenantRepository.countByUpload1IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file1");
        count = tenantRepository.countByUpload2IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file2");
        count = tenantRepository.countByUpload3IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file3");
        count = tenantRepository.countByUpload4IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file4");
        count = tenantRepository.countByUpload5IsNotNullTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "file5");
        count = tenantRepository.countByFilesNotUploadedTenantGuarantor();
        UtilsLocatio.extractStatistics(map, count, "notUpload");

        return map;
    }

    public double countOverallSatisfaction() {
        List<Object> objects = tenantRepository.tenantsSatisfactionStatistics();
        if (objects == null || objects.isEmpty()) {
            return 0;
        }
        double satisfaction = 0;
        double insatisfaction = 0;
        for (Object object : objects) {
            if (((Object[]) object)[1] != null && (Integer) ((Object[]) object)[1] == 1) {
                satisfaction++;
            } else if (((Object[]) object)[1] != null && (Integer) ((Object[]) object)[1] == -1) {
                insatisfaction++;
            }
        }
        return (satisfaction + insatisfaction > 0) ? ((long) satisfaction / (satisfaction + insatisfaction)) : 0;
    }

    public Long countUploadedFiles() {
        return tenantRepository.countTotalUploadedFiles();
    }

    public Tenant findOneByEmail(String email) {
        return tenantRepository.findOneByEmail(email);
    }

    public void save(Tenant tenant, TenantDTO tenantDTO) {
        tenant.setFirstName(tenantDTO.getFirstName());
        tenant.setLastName(tenantDTO.getLastName());
        tenantRepository.save(tenant);
    }
}


