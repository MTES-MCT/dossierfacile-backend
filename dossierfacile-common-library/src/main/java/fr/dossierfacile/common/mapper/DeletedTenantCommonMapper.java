package fr.dossierfacile.common.mapper;

import com.google.common.hash.Hashing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.DeletedTenantModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Mapper(componentModel = "spring")
public interface DeletedTenantCommonMapper {
    @Mapping(target = "hashedEmail", source = "email", qualifiedByName = "emailToHashedEmail")
    public abstract DeletedTenantModel toDeletedTenantModel(Tenant tenant);
    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? document.getWatermarkFile().getName() : null)")
    @Mapping(target = "subCategory", source = "documentSubCategory")
    @HideNewSubCategories
    public abstract DocumentModel toDocumentModel(Document document);

    @Named("emailToHashedEmail")
    static String emailToHashedEmail(String email) {
        return Hashing.sha256().hashString(email, StandardCharsets.UTF_8).toString();
    }
}
