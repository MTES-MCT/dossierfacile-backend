package fr.dossierfacile.api.dossierfacileapiowner.log;

import com.google.common.hash.Hashing;
import fr.dossierfacile.common.entity.Owner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Mapper(componentModel = "spring")
public interface DeletedOwnerMapper {

    @Named("hashString")
    static String hash(String value) {
        if (value == null) {
            return "";
        }
        return Hashing.sha256().hashString(value, StandardCharsets.UTF_8).toString();
    }

    @Mapping(target = "hEmail", source = "email", qualifiedByName = "hashString")
    @Mapping(target = "hFirstName", source = "firstName", qualifiedByName = "hashString")
    @Mapping(target = "hLastName", source = "lastName", qualifiedByName = "hashString")
    @Mapping(target = "hPreferredName", source = "preferredName", qualifiedByName = "hashString")
    DeletedOwnerModel toDeletedOwnerModel(Owner owner);
}
