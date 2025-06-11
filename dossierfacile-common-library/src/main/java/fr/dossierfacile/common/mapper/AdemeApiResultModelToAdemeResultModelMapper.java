package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.model.AdemeResultModel;
import fr.dossierfacile.common.model.ademe.AdemeApiResultModel;
import fr.dossierfacile.common.model.ademe.housing.AdemeApiDpeHousingJson;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AdemeApiResultModelToAdemeResultModelMapper {

    public AdemeResultModel convert(AdemeApiResultModel input) {
        if (input == null) {
            return null;
        }

        var logement = getLogement(input);

        return AdemeResultModel.builder()
                .numero(input.getDpe().getNumeroDpe())
                .statut(input.getDpe().getStatut())
                .anneeConstruction(logement.getCaracteristiqueGenerale().getAnneeConstruction())
                .adresse(input.getDpe().getAdministratif().getGeolocalisation().getAdresses().getAdresseBien().getAdresseBrut())
                .dpe2012(false)
                .emission(logement.getSortie().getEmissionGes().getEmissionGes5UsagesM2())
                .anonymise(false)
                .codePostal(input.getDpe().getAdministratif().getGeolocalisation().getAdresses().getAdresseBien().getCodePostalBrut())
                .masquerDiag(true)
                .consommation(logement.getSortie().getEpConso().getEpConso5UsagesM2())
                .typeBatiment(getTypeBatiment(logement))
                .etiquetteBilan(logement.getSortie().getEpConso().getClasseBilanDpe())
                .dateFinValidite(getDateFinValidite(input))
                .dateRealisation(getDateRealisation(input))
                .surfaceHabitable(logement.getCaracteristiqueGenerale().getSurfaceHabitableLogement())
                .etiquetteEmission(logement.getSortie().getEmissionGes().getClasseEmissionGes())
                .suppressionEnCours(false)
                .consommationEnergieFinale(input.getConsommationEnergieFinale().toString()).build();
    }

    private AdemeApiDpeHousingJson getLogement(AdemeApiResultModel input) {
        if (input.getDpe().getLogementNeuf() != null) {
            return input.getDpe().getLogementNeuf();
        } else {
            return input.getDpe().getLogement();
        }
    }

    private String getTypeBatiment(AdemeApiDpeHousingJson input) {
        if(input.getCaracteristiqueGenerale().getNombreAppartement() == null) {
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
