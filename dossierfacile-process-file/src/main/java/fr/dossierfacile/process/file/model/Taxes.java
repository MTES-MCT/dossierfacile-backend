package fr.dossierfacile.process.file.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Taxes {

    // Deprecated
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
    // Deprecated



    private int year;
    private String rfr;
    private String sitFam;
    private Integer nbPart;
    private Pac pac;
    private String nmNaiDec1;
    private String nmUsaDec1;
    private String prnmDec1;
    private ApiDate dateNaisDec1;
    private String nmNaiDec2;
    private String nmUsaDec2;
    private String prnmDec2;
    private ApiDate dateNaisDec2;

    private String aft;
    private AftDetail aftDetail;

    private List<Facture> facture;

    private String getName(String birthname, String lastname, String firstname) {
        if (birthname == null || firstname == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(lastname)) {
            return "name: " + birthname + " " + firstname + ", nameOfBirth: " + lastname;
        }
        return "name: " + birthname + " " + firstname;
    }

    public String getDeclarant1Name() {
        return getName(nmNaiDec1, nmUsaDec1, prnmDec1);
    }

    public String getDeclarant2Name() {
        return getName(nmNaiDec2, nmUsaDec2, prnmDec2);
    }

}
