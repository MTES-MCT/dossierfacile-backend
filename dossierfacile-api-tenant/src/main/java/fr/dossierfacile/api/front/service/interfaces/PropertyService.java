package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Property;

public interface PropertyService {

    Property getPropertyByToken(String token);

}
