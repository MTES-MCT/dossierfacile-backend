package fr.gouv.owner.dto;


import java.time.LocalDateTime;


public interface TenantBOIndexDTO {

    Integer getId();

    LocalDateTime getCreationDateTime();

    String getFirstName();

    String getLastName();

    String getEmail();

    Integer getUserApiId();

    String getPartnerId();
}
