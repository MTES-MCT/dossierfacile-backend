package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.log.LogService;
import fr.dossierfacile.api.dossierfacileapiowner.register.AuthenticationFacade;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerMapper;
import fr.dossierfacile.api.dossierfacileapiowner.user.OwnerModel;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/property")
public class PropertyController {

    private final LogService logService;
    private final PropertyService propertyService;
    private final PropertyApartmentSharingService propertyApartmentSharingService;
    private final AuthenticationFacade authenticationFacade;
    private final OwnerMapper ownerMapper;

    @PutMapping
    public ResponseEntity<PropertyModel> createOrUpdate(@RequestBody PropertyForm Property) {
        PropertyModel propertyModel = propertyService.createOrUpdate(Property);
        logService.saveLog(LogType.ACCOUNT_EDITED, propertyModel.getId());
        return ok(propertyModel);
    }

    @GetMapping
    public ResponseEntity<List<PropertyModel>> getAll() {
        List<PropertyModel> properties = propertyService.getAllProperties();
        return ok(properties);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OwnerModel> delete(@PathVariable Long id) {
        propertyService.delete(id);
        Owner owner = authenticationFacade.getOwner();
        return ok(ownerMapper.toOwnerModel(owner));
    }

    @GetMapping(value = "/listAddresses/{query}")
    public ResponseEntity<String> getAddresses(@PathVariable String query) throws HttpResponseException {
        try {
            URI uri = new URIBuilder("https://api-adresse.data.gouv.fr/search/").addParameter("q", query).addParameter("limit", "15").build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return ok(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        throw new HttpResponseException(404, "No result");
    }

    @DeleteMapping("/tenant/{id}")
    public void removeTenant(@PathVariable("id") Long id) throws HttpResponseException {
        Owner owner = authenticationFacade.getOwner();
        List<Property> propertyList = owner.getProperties();
        List<PropertyApartmentSharing> propertyApartmentSharings = propertyList.stream().flatMap(property -> property.getPropertiesApartmentSharing().stream()).collect(Collectors.toList());
        PropertyApartmentSharing propertyApartmentSharing = propertyApartmentSharings.stream().filter(pas ->
                pas.getId().equals(id)).findAny().orElse(null);

        if (propertyApartmentSharing == null) {
            throw new HttpResponseException(404, "No tenant found");
        }
        propertyApartmentSharingService.deletePropertyApartmentSharing(propertyApartmentSharing);
    }
}
