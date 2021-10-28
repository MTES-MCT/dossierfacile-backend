package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.type.TaxDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GuarantorItem {
    private Long guarantorId;
    private TypeGuarantor typeGuarantor;
    private String firstName;
    private String lastName;
    private String legalPersonName;
    @Builder.Default
    private List<MessageItem> messageItems = new ArrayList<>();
}
