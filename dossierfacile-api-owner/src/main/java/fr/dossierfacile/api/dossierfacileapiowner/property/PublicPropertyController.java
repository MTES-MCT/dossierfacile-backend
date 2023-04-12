package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/property/public")
@Slf4j
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

    @PostMapping("/subscribe/{propertyToken}")
    public ResponseEntity<Object> subscribe(@PathVariable String propertyToken, @Valid @RequestBody SubscriptionApartmentSharingOfTenantForm subscribeForm) throws HttpResponseException, InterruptedException {
        try {
            propertyService.subscribeTenantToProperty(propertyToken, subscribeForm.getKcToken());
            return ok().build();
        } catch (Exception e) {
            log.error("Couldn't subscribe", e);
            throw new HttpResponseException(403, "Couldn't subscribe " + e.getMessage());
        }
    }
}
