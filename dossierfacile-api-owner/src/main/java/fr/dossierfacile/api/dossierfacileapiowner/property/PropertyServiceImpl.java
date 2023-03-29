package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final AuthenticationFacade authenticationFacade;
    private final PropertyRepository propertyRepository;
    private final OwnerPropertyMapper propertyMapper;

    @Value("${keycloak.partner.client.id}")
    private String clientId;
    @Value("${keycloak.partner.client.secret}")
    private String clientSecret;
    @Value("${keycloak.partner.keycloak.token.url}")
    private String keycloakTokenUrl;
    @Value("${tenant.api.url}")
    private String tenantApiUrl;

    @Override
    public PropertyModel createOrUpdate(PropertyForm propertyForm) {
        Owner owner = authenticationFacade.getOwner();
        Property property;
        if (propertyForm.getId() != null) {
             property = propertyRepository.findByIdAndOwnerId(propertyForm.getId(), owner.getId()).orElse(new Property());
        } else {
            property = new Property();
            property.setName("Propriété");
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
        if (propertyForm.getCo2Emission() != null && propertyForm.getCo2Emission() >= 0) {
            property.setCo2Emission(propertyForm.getCo2Emission());
        }
        if (propertyForm.getEnergyConsumption() != null && propertyForm.getEnergyConsumption() >= 0) {
            property.setEnergyConsumption(propertyForm.getEnergyConsumption());
        }
        if (propertyForm.getValidated() != null && propertyForm.getValidated()) {
            property.setValidated(true);
        }
        property.setOwner(owner);
        return propertyMapper.toPropertyModel(propertyRepository.save(property));
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
    }

    @Override
    public Optional<Property> getProperty(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public Optional<Property> getPropertyByToken(String token) {
        return propertyRepository.findByToken(token);
    }

    @Override
    public HttpResponse<String> subscribeTenantToProperty(String token, Long tenantId) throws IOException, InterruptedException {
        String openIdToken;
        try {
            openIdToken = getOpenIdToken();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        } catch (Exception e) {
            throw new HttpResponseException(500, "Couldn't get token");
        }

        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("https").host(tenantApiUrl).path("/api-partner/tenant/{tenantId}/subscribe/{token}")
                .buildAndExpand(tenantId, token);

        Map<String, String> data = new HashMap<>();
        data.put("access", "true");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriComponents.toUri())
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+openIdToken)
                .build();
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getOpenIdToken() throws IOException, InterruptedException, URISyntaxException {
        String data = "grant_type=client_credentials&client_id="+clientId+"&client_secret="+clientSecret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(keycloakTokenUrl))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new HttpResponseException(500, "Couldn't get token");
        }

        @SuppressWarnings("unchecked")
        HashMap<String, Object> body =  new ObjectMapper().readValue(response.body(), HashMap.class);

        return (String) body.get("access_token");
    }
}
