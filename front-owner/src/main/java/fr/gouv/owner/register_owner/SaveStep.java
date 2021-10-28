package fr.gouv.owner.register_owner;

import fr.gouv.owner.dto.OwnerDTO;
import fr.dossierfacile.common.entity.Owner;

public interface SaveStep {
    Owner saveStep(OwnerDTO ownerDTO);
}
