package fr.gouv.bo.model;

import fr.dossierfacile.common.enums.Role;

public record RoleDTO(
        String displayValue,
        Role value
){}
