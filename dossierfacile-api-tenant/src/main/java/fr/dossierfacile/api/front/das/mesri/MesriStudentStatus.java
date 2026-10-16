package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Student status returned by API Particulier ({@code /v3/mesri/statut_etudiant/france_connect}).
 *
 * @param rawJson        the response body exactly as received (rendered in full in the PDF)
 * @param sourceUrl      the URL that was called, without the query string
 * @param recipientSiret the SIRET passed as {@code recipient}
 * @param admissions     parsed {@code data.admissions[]}
 */
public record MesriStudentStatus(String rawJson, String sourceUrl, String recipientSiret, List<Admission> admissions) {

    public record Admission(String dateDebut, String dateFin, Boolean estInscrit, String regimeCode, String regimeLibelle,
            String communeCode, String uai, String etablissementNom) {

        static Admission from(JsonNode node) {
            return new Admission(
                    node.path("date_debut").asText(null),
                    node.path("date_fin").asText(null),
                    node.hasNonNull("est_inscrit") ? node.get("est_inscrit").asBoolean() : null,
                    node.path("regime_formation").path("code").asText(null),
                    node.path("regime_formation").path("libelle").asText(null),
                    node.path("code_cog_insee_commune").asText(null),
                    node.path("etablissement_etudes").path("uai").asText(null),
                    node.path("etablissement_etudes").path("nom").asText(null));
        }
    }

    static List<Admission> parseAdmissions(JsonNode root) {
        List<Admission> result = new ArrayList<>();
        for (JsonNode node : root.path("data").path("admissions")) {
            result.add(Admission.from(node));
        }
        return result;
    }
}
