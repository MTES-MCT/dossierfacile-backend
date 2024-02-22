package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.common.entity.UserApi;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface PartnerSettingsMapper {
    PartnerSettings toPartnerSettings(UserApi userApi);
}