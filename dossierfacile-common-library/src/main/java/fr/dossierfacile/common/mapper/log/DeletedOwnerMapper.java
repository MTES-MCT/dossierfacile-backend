package fr.dossierfacile.common.mapper.log;

import com.google.common.hash.Hashing;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.model.log.DeletedOwnerModel;
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

    @Mapping(target = "hashedEmail", source = "email", qualifiedByName = "hashString")
    @Mapping(target = "hashedLastname", source = "lastName", qualifiedByName = "hashString")
    @Mapping(target = "hashedFirstname", source = "firstName", qualifiedByName = "hashString")
    @Mapping(target = "hashedPreferredName", source = "preferredName", qualifiedByName = "hashString")
    DeletedOwnerModel toDeletedOwnerModel(Owner owner);
}
