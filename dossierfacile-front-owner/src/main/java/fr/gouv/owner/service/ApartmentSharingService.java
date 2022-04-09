package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.repository.ApartmentSharingRepository;
import fr.gouv.owner.utils.UtilsLocatio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApartmentSharingService {

    @Autowired
    private ApartmentSharingRepository apartmentSharingRepository;

    public void createApartmentSharing(Prospect prospect) {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.addProspect(prospect);
        apartmentSharing.setToken(UtilsLocatio.generateRandomString(8));
        apartmentSharing.setTokenPublic(UtilsLocatio.generateRandomString(8));
        apartmentSharingRepository.save(apartmentSharing);
    }

    public ApartmentSharing findApartmentSharingByBothToken(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findOneByTokenPublic(token);
        if (apartmentSharing != null) {
            return apartmentSharing;
        }
        apartmentSharing = apartmentSharingRepository.findOneByToken(token);
        return apartmentSharing;
    }

    public ApartmentSharing find(int id) {
        return apartmentSharingRepository.getOne(id);
    }

    public void save(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.save(apartmentSharing);
    }

}
