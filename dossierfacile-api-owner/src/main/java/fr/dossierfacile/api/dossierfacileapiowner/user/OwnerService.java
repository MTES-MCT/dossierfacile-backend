package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.Owner;

public interface OwnerService {

    OwnerModel setNames(NamesForm namesForm);

    void deleteAccount(Owner owner);
}
