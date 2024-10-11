package fr.dossierfacile.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiResultModel {

//    {"id":null
//"type":"DPE 3CL 2021 méthode logement"
private String numero;
private String adresse;
private String codePostal;
private String Tours;
//"region":null
//"departement":null
//"epci":null
private String typeBatiment;
private String anneeConstruction;
private String surfaceHabitable;
//"diagnostiqueurNom":null
//"diagnostiqueurPrenom":null
//"diagnostiqueurFullName":null
//"diagnostiqueurCertificat":null
private String dateRealisation;
private String dateFinValidite;
private String consommation;
private String consommationEnergieFinale;
private String emission;
private String etiquetteEmission;
//"etiquetteEmissionPetiteSurface":null
//"etiquetteEnergiePetiteSurface":null
private String etiquetteBilan;
//"etiquetteBilanPetiteSurface":null
//"typeGenerateurEcs":["Chaudière gaz à condensation 2001-2015"]
//"typeEnergieEcs":["Gaz naturel"]
//"typeGenerateurCh":["Chaudière gaz à condensation 2001-2015"]
//"typeEnergieCh":["Gaz naturel"]
private String numeroDPERemplace;
private String motifRemplacement;
private String statut;
private Boolean masquerDiag;
private String numeroDPERemplacant;
private Boolean anonymise;
private Boolean suppressionEnCours;
private Boolean dpe2012;

}
