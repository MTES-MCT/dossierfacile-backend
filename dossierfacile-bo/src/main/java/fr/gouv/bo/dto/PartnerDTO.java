package fr.gouv.bo.dto;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnerDTO {
    private Long partner;
    private String internalPartnerId;
}
