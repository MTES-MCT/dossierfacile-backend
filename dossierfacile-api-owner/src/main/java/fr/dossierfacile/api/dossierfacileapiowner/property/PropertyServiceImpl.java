package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.dossierfacileapiowner.log.OwnerLogService;
import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.api.dossierfacileapiowner.register.DPENotFoundException;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.repository.PropertyLogRepository;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.utils.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final AuthenticationFacade authenticationFacade;
    private final PropertyRepository propertyRepository;
    private final OwnerPropertyMapper propertyMapper;
    private final PropertyApartmentSharingService propertyApartmentSharingService;
    private final TenantCommonService tenantService;
    private final PropertyLogRepository propertyLogRepository;
    private final OwnerLogService ownerLogService;
    private final MailService mailService;
    private final ObjectMapper objectMapper = MapperUtil.newObjectMapper();

    @Qualifier("tenantJwtDecoder")
    @Autowired
    private JwtDecoder tenantJwtDecoder;

    @Override
    public PropertyModel createOrUpdate(PropertyForm propertyForm) throws HttpResponseException, InterruptedException {
        Owner owner = authenticationFacade.getOwner();
        Property property;
        if (propertyForm.getId() != null) {
            property = propertyRepository.findByIdAndOwnerId(propertyForm.getId(), owner.getId()).orElse(new Property());
        } else {
            property = new Property();
            property.setName("Propriété");
            ownerLogService.saveLog(OwnerLogType.PROPERTY_CREATED, owner.getId());
        }
        if (propertyForm.getName() != null && !propertyForm.getName().isBlank()) {
            property.setName(propertyForm.getName());
        }
        if (propertyForm.getType() != null) {
            property.setType(propertyForm.getType());
        }
        if (propertyForm.getAddress() != null) {
            property.setAddress(propertyForm.getAddress());
        }
        if (propertyForm.getFurniture() != null) {
            property.setFurniture(propertyForm.getFurniture());
        }
        if (propertyForm.getRentCost() != null && propertyForm.getRentCost() >= 0) {
            property.setRentCost(propertyForm.getRentCost());
        }
        if (propertyForm.getChargesCost() != null && propertyForm.getChargesCost() >= 0) {
            property.setChargesCost(propertyForm.getChargesCost());
        }
        if (propertyForm.getLivingSpace() != null && propertyForm.getLivingSpace() >= 0) {
            property.setLivingSpace(propertyForm.getLivingSpace());
        }
        if (propertyForm.getAdemeNumber() != null) {
            setAdemeResult(propertyForm, property);
        } else {
            if (propertyForm.getCo2Emission() != null && propertyForm.getCo2Emission() >= 0) {
                property.setCo2Emission(propertyForm.getCo2Emission());
            }
            if (propertyForm.getEnergyConsumption() != null && propertyForm.getEnergyConsumption() >= 0) {
                property.setEnergyConsumption(propertyForm.getEnergyConsumption());
            }
            if (propertyForm.getDpeDate() != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    property.setDpeDate(format.parse(propertyForm.getDpeDate()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (propertyForm.getValidated() != null && propertyForm.getValidated()) {
            property.setValidated(true);
            property.setValidatedDate(LocalDateTime.now());
            mailService.sendEmailValidatedProperty(owner, property);
            ownerLogService.saveLog(OwnerLogType.PROPERTY_COMPLETED, owner.getId());
        }
        property.setOwner(owner);
        return propertyMapper.toPropertyModel(propertyRepository.save(property));
    }

    private void setAdemeResult(PropertyForm propertyForm, Property property) throws HttpResponseException, InterruptedException {
        URI uri;
        HttpResponse<String> response;
        try {
            uri = new URI("https://observatoire-dpe-audit.ademe.fr/pub/dpe/" + propertyForm.getAdemeNumber());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                AdemeApiResultModel ademeApiResultModel = objectMapper.readValue(json, AdemeApiResultModel.class);

                property.setAdemeNumber(ademeApiResultModel.getNumero());
                ObjectMapper mapper = new ObjectMapper();
                property.setAdemeApiResult(mapper.valueToTree(ademeApiResultModel));
                property.setEnergyConsumption(Float.valueOf(ademeApiResultModel.getConsommation()).intValue());
                property.setCo2Emission(Float.valueOf(ademeApiResultModel.getEmission()).intValue());
                Instant instant = Instant.parse(ademeApiResultModel.getDateRealisation());
                Date dateRealisation = Date.from(instant);
                property.setDpeDate(dateRealisation);
                if (response.statusCode() == 404) {
                    throw new DPENotFoundException("DPE not found");
                }
                return;
            } catch (IOException e) {
                log.error("An error occurred while processing the request", e);
            }
        } catch (URISyntaxException e) {
            log.error("An error occurred while processing the request", e);
        }
        throw new HttpResponseException(500, "An error occured processing ademe api request");
    }

    @Override
    public List<PropertyModel> getAllProperties() {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        return properties.stream().map(propertyMapper::toPropertyModel).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        Owner owner = authenticationFacade.getOwner();
        List<Property> properties = propertyRepository.findAllByOwnerId(owner.getId());
        Optional<Property> property = properties.stream().filter(p -> p.getId().equals(id)).findFirst();
        property.ifPresent(propertyRepository::delete);
        ownerLogService.saveLog(OwnerLogType.PROPERTY_DELETED, owner.getId());
    }

    @Override
    public Optional<Property> getProperty(Long id) {
        Owner owner = authenticationFacade.getOwner();
        return propertyRepository.findByIdAndOwnerId(id, owner.getId());
    }

    @Override
    public Optional<Property> getPropertyByToken(String token) {
        return propertyRepository.findByToken(token);
    }

    @Override
    public void subscribeTenantToProperty(String propertyToken, String kcTenantToken) {
        // get tenant from jwt, then tenant give his consent
        Jwt jwt = tenantJwtDecoder.decode(kcTenantToken);
        String tenantKeycloakId = jwt.getClaimAsString("sub");
        Tenant tenant = tenantService.findByKeycloakId(tenantKeycloakId);

        Property property = getPropertyByToken(propertyToken).get();

        propertyApartmentSharingService.subscribeTenantApartmentSharingToProperty(tenant, property, true);
    }

    @Override
    public void logAccess(Property property) {
        PropertyLog log = PropertyLog.applicationPageVisited(property);
        propertyLogRepository.save(log);
    }

    @Override
    public void deleteDpe(Property property) {
        property.setAdemeApiResult(null);
        property.setAdemeNumber(null);
        property.setCo2Emission(null);
        property.setEnergyConsumption(null);
        property.setDpeDate(null);
        propertyRepository.save(property);
    }

}
