package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import org.apache.http.client.HttpResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/property/public")
public class PublicPropertyController {

    private final PropertyService propertyService;
    private final OwnerPropertyMapper ownerPropertyMapper;

    @GetMapping("/{token}")
    public ResponseEntity<LightPropertyModel> get(@PathVariable String token) throws HttpResponseException {
        Optional<Property> property = propertyService.getPropertyByToken(token);
        if (property.isPresent()) {
            return ok(ownerPropertyMapper.toLightPropertyModel(property.get()));
        }
        throw new HttpResponseException(404, "No property found");
    }

    @PostMapping("/subscribe/{token}/{tenantId}")
    public ResponseEntity<Object> subscribe(@PathVariable String token, @PathVariable Long tenantId ) throws HttpResponseException {
        try {
            propertyService.subscribeTenantToProperty(token, tenantId);
            return ok().build();
        } catch (HttpResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpResponseException(403, "Couldn't subscribe");
        }
    }

}
