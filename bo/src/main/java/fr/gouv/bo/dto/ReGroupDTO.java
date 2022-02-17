package fr.gouv.bo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReGroupDTO {
    private String tenantEmailCreate;
    private String tenantEmailJoin;
    private String tenantType;
}
