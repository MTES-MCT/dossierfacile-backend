package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.log.LogService;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/property")
public class PropertyController {

    private final LogService logService;
    private final PropertyService propertyService;

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

}
