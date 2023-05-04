package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.gouv.bo.dto.ApartmentSharingDTO01;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApartmentSharingService {
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final ApartmentSharingCommonService apartmentSharingCommonService;

    public ApartmentSharing find(Long id) {
        return apartmentSharingCommonService.findById(id).get();
    }

    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.delete(apartmentSharing);
    }

    public void save(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.save(apartmentSharing);
    }

    public Page<ApartmentSharingDTO01> findAll(Pageable pageable) {
        return apartmentSharingRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing);
    }

    public void refreshUpdateDate(ApartmentSharing apartmentSharing) {
        apartmentSharing.setLastUpdateDate(LocalDateTime.now());
        apartmentSharingRepository.save(apartmentSharing);
    }
}
