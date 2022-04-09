package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.LinkLogRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.OvhService;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingServiceImpl implements ApartmentSharingService {

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final TenantCommonRepository tenantRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final ApplicationLightMapper applicationLightMapper;
    private final OvhService ovhService;
    private final LinkLogRepository linkLogRepository;
    private final Producer producer;

    @Override
    public void createApartmentSharing(Tenant tenant) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId()).orElse(new ApartmentSharing(tenant));
        apartmentSharingRepository.save(apartmentSharing);
        tenant.setApartmentSharing(apartmentSharing);
        tenantRepository.save(tenant);
    }

    @Override
    public ApplicationModel full(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));
        saveLinkLog(apartmentSharing, token, LinkType.FULL_APPLICATION);
        return applicationFullMapper.toApplicationModel(apartmentSharing);
    }

    @Override
    public ApplicationModel light(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTokenPublic(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));
        saveLinkLog(apartmentSharing, token, LinkType.LIGHT_APPLICATION);
        return applicationLightMapper.toApplicationModel(apartmentSharing);
    }


    @Override
    public ByteArrayOutputStream fullPdf(String token) throws IOException {
        ByteArrayOutputStream outputStreamResult = new ByteArrayOutputStream();

        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));

        saveLinkLog(apartmentSharing, token, LinkType.DOCUMENT);

        String urlDossierPdfDocument = apartmentSharing.getUrlDossierPdfDocument();
        if (urlDossierPdfDocument == null) {
            throw new FileNotFoundException("Full PDF doesn't exist yet");
        } else {
            SwiftObject swiftObject = ovhService.get(urlDossierPdfDocument);
            if (swiftObject != null) {
                DLPayload dlPayload = swiftObject.download();
                if (dlPayload.getHttpResponse().getStatus() == HttpStatus.OK.value()) {
                    log.info("Dossier PDF downloaded for ApartmentSharing with ID [" + apartmentSharing.getId() + "]");
                    InputStream fileIS = swiftObject.download().getInputStream();
                    IOUtils.copy(fileIS, outputStreamResult);
                } else {
                    log.error("Problem downloading Dossier pdf [" + urlDossierPdfDocument + "].");
                }
            }
        }
        return outputStreamResult;
    }

    @Override
    @Transactional
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        String currentUrl = apartmentSharing.getUrlDossierPdfDocument();
        if (currentUrl != null) {
            ovhService.delete(currentUrl);
            apartmentSharing.setUrlDossierPdfDocument(null);
            apartmentSharingRepository.save(apartmentSharing);
        }
    }

    private void saveLinkLog(ApartmentSharing apartmentSharing, String token, LinkType linkType) {
        linkLogRepository.save(new LinkLog(
                apartmentSharing,
                token,
                linkType
        ));
    }

    @Override
    public void createFullPdf(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token).orElseThrow(() -> new ApartmentSharingNotFoundException(token));

        saveLinkLog(apartmentSharing, token, LinkType.DOCUMENT);

        String urlDossierPdfDocument = apartmentSharing.getUrlDossierPdfDocument();
        if (urlDossierPdfDocument == null) {
            producer.generateFullPdf(apartmentSharing.getId());
        }
    }

}
