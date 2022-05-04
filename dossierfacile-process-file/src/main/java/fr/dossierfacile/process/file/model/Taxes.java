package fr.dossierfacile.process.file.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Taxes {
    private Declarant declarant1;
    private Declarant declarant2;
    private FoyerFiscal foyerFiscal;
    private String dateRecouvrement;
    private String dateEtablissement;
    private String nombreParts;
    private int revenuBrutGlobal;
    private int revenuImposable;
    private int impotRevenuNetAvantCorrections;
    private int montantImpot;
    private int revenuFiscalReference;
    private int nombrePersonnesCharge;
    private String anneeImpots;
    private String anneeRevenus;
    private String erreurCorrectif;
    private String situationPartielle;
    private String situationFamille;
}
