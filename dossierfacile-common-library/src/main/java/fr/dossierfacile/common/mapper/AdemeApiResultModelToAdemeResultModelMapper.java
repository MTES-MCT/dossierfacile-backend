package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.model.AdemeResultModel;
import fr.dossierfacile.common.model.ademe.AdemeApiResultModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AdemeApiResultModelToAdemeResultModelMapper {

    public AdemeResultModel convert(AdemeApiResultModel input) {
        if (input == null) {
            return null;
        }

        return AdemeResultModel.builder()
                .numero(input.getDpe().getNumeroDpe())
                .statut(input.getDpe().getStatut())
                .anneeConstruction(input.getDpe().getLogement().getCaracteristiqueGenerale().getAnneeConstruction())
                .adresse(input.getDpe().getAdministratif().getGeolocalisation().getAdresses().getAdresseBien().getAdresseBrut())
                .dpe2012(false)
                .emission(input.getDpe().getLogement().getSortie().getEmissionGes().getEmissionGes5UsagesM2())
                .anonymise(false)
                .codePostal(input.getDpe().getAdministratif().getGeolocalisation().getAdresses().getAdresseBien().getCodePostalBrut())
                .masquerDiag(true)
                .consommation(input.getDpe().getLogement().getSortie().getEpConso().getEpConso5UsagesM2())
                .typeBatiment(getTypeBatiment(input))
                .etiquetteBilan(input.getDpe().getLogement().getSortie().getEpConso().getClasseBilanDpe())
                .dateFinValidite(getDateFinValidite(input))
                .dateRealisation(getDateRealisation(input))
                .surfaceHabitable(input.getDpe().getLogement().getCaracteristiqueGenerale().getSurfaceHabitableLogement())
                .etiquetteEmission(input.getDpe().getLogement().getSortie().getEmissionGes().getClasseEmissionGes())
                .suppressionEnCours(false)
                .consommationEnergieFinale(input.getConsommationEnergieFinale().toString()).build();
    }

    private String getTypeBatiment(AdemeApiResultModel input) {
        if(input.getDpe().getLogement().getCaracteristiqueGenerale().getNombreAppartement() == null) {
            return "maison";
        } else {
            return "appartement";
        }
    }

    private String getDateRealisation(AdemeApiResultModel input) {
        var date = input.getDpe().getAdministratif().getDateEtablissementDpe();
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            var formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            var formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            var localDate = LocalDate.parse(date, formatterInput);
            var zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
            return formatterOutput.format(zonedDateTime);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date, e);
        }
    }

    private String getDateFinValidite(AdemeApiResultModel input) {
        var date = input.getDpe().getAdministratif().getDateEtablissementDpe();
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            var formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            var formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            var localDate = LocalDate.parse(date, formatterInput);
            var zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
            var newDate = zonedDateTime.plusYears(10);
            return formatterOutput.format(newDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + date, e);
        }
    }

}
