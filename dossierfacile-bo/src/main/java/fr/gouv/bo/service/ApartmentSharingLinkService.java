package fr.gouv.bo.service;

import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApartmentSharingLinkService {
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;

    public void delete(Long linkId) {
        apartmentSharingLinkRepository.deleteById(linkId);
    }
}
