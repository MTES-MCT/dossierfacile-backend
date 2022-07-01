package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import lombok.AllArgsConstructor;
import org.apache.http.client.HttpResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public ResponseEntity<PropertyModel> get(@PathVariable Long id) throws HttpResponseException {
        Optional<Property> property = propertyService.getProperty(id);
        if (property.isPresent()) {
            return ok(ownerPropertyMapper.toPropertyModel(property.get()));
        }
        throw new HttpResponseException(404, "No property found");
    }

}
