package fr.dossierfacile.common.mapper.log;

import com.google.common.hash.Hashing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.log.DeletedTenantModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Mapper(componentModel = "spring")
public interface DeletedTenantMapper {
    @Named("hashString")
    static String hash(String value) {
        if (value == null) {
            return "";
        }
        return Hashing.sha256().hashString(value, StandardCharsets.UTF_8).toString();
    }

    @Mapping(target = "hashedEmail", source = "email", qualifiedByName = "hashString")
    @Mapping(target = "hashedLastname", source = "lastName", qualifiedByName = "hashString")
    @Mapping(target = "hashedFirstname", source = "firstName", qualifiedByName = "hashString")
    @Mapping(target = "hashedPreferredName", source = "preferredName", qualifiedByName = "hashString")
    @Mapping(target = "applicationType", expression = "java((tenant.getApartmentSharing() != null )? tenant.getApartmentSharing().getApplicationType() : null)")
    @Mapping(target = "apartmentSharingId", expression = "java((tenant.getApartmentSharing() != null )? tenant.getApartmentSharing().getId() : null)")
    DeletedTenantModel toDeletedTenantModel(Tenant tenant);

    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? document.getWatermarkFile().getName() : null)")
    DocumentModel toDocumentModel(Document document);
}
