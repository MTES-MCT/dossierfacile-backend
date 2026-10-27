package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Student status returned by API Particulier ({@code /v3/mesri/statut_etudiant/france_connect}).
 *
 * @param rawJson        the response body exactly as received
 * @param sourceUrl      the URL that was called, without the query string
 * @param recipientSiret the SIRET passed as {@code recipient}
 * @param identite       {@code data.identite} when the {@code mesri_identite} scope is served (absent from the sandbox FC cases), else null
 * @param admissions     parsed {@code data.admissions[]}
 */
public record MesriStudentStatus(String rawJson, String sourceUrl, String recipientSiret, Identity identite,
        List<Admission> admissions) {

    public record Identity(String nomNaissance, String prenom, String dateNaissance) {
        static Identity from(JsonNode node) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            return new Identity(node.path("nom_naissance").asText(null), node.path("prenom").asText(null),
                    node.path("date_naissance").asText(null));
        }
    }

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

    static Identity parseIdentity(JsonNode root) {
        return Identity.from(root.path("data").path("identite"));
    }
}
