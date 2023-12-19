package fr.dossierfacile.common.entity.ocr;

import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TaxIncomeMainFile implements ParsedFile, Serializable {
    @Builder.Default
    ParsedFileClassification classification = ParsedFileClassification.TAX_INCOME;
    String declarant1NumFiscal;
    String declarant1Nom;
    String declarant2NumFiscal;
    String declarant2Nom;
    String nombreDeParts;
    String anneeDesRevenus;
    String dateDeMiseEnRecouvrement;
    String revenuFiscalDeReference;
    String numeroFiscalDeclarant1;
    String numeroFiscalDeclarant2;
    String referenceAvis;
    List<TaxIncomeLeaf> taxIncomeLeaves;
}
