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
    @Mapping(target = "hashedLastname", source = "lastName", qualifiedByName = "lastnameToHashedLastname")
    @Mapping(target = "hashedFirstname", source = "firstName", qualifiedByName = "firstnameToHashedFirstname")
    @Mapping(target = "hashedPreferredName", source = "preferredName", qualifiedByName = "preferredNameToHashedPreferredName")
    public abstract DeletedTenantModel toDeletedTenantModel(Tenant tenant);
    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? document.getWatermarkFile().getName() : null)")
    @Mapping(target = "subCategory", source = "documentSubCategory")
    @HideNewSubCategories
    public abstract DocumentModel toDocumentModel(Document document);

    @Named("emailToHashedEmail")
    static String emailToHashedEmail(String email) {
        if (email == null) {
            return "";
        }
        return Hashing.sha256().hashString(email, StandardCharsets.UTF_8).toString();
    }
    @Named("firstnameToHashedFirstname")
    static String firstnameToHashedFirstname(String firstname) {
        if (firstname == null) {
            return "";
        }
        return Hashing.sha256().hashString(firstname, StandardCharsets.UTF_8).toString();
    }
    @Named("lastnameToHashedLastname")
    static String lastnameToHashedLastname(String lastname) {
        if (lastname == null) {
            return "";
        }
        return Hashing.sha256().hashString(lastname, StandardCharsets.UTF_8).toString();
    }

    @Named("preferredNameToHashedPreferredName")
    static String preferredNameToHashedPreferredName(String preferredName) {
        if (preferredName == null) {
            return "";
        }
        return Hashing.sha256().hashString(preferredName, StandardCharsets.UTF_8).toString();
    }
}
