package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PropertyApartmentSharingServiceImpl implements PropertyApartmentSharingService {
    private final PropertyApartmentSharingRepository propertyApartmentSharingRepository;

    @Override
    public void deletePropertyApartmentSharing(PropertyApartmentSharing propertyApartmentSharing) {
        propertyApartmentSharingRepository.delete(propertyApartmentSharing);

    }
}
