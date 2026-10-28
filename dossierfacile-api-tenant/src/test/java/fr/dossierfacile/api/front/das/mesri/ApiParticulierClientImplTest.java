package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiParticulierClientImplTest {

    /** Payload of the DINUM sandbox test case fake_france_connect_mesri.yml. */
    static final String SANDBOX_BODY = """
            {"data":{"admissions":[{"date_debut":"2020-07-01","date_fin":"2021-08-31","est_inscrit":true,
            "regime_formation":{"libelle":"formation initiale","code":"RF1"},"code_cog_insee_commune":"33063",
            "etablissement_etudes":{"uai":"0330023W","nom":"LYCEE GENERAL ET TECHNOLOGIQUE CAMILLE JULLIAN"}}]},
            "links":{},"meta":{}}""";

    private HttpClient httpClient;
    private ApiParticulierClientImpl client;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        client = new ApiParticulierClientImpl("https://staging.particulier.api.gouv.fr", "12006701200027", httpClient, new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private void answer(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test
    void parsesAdmissionsAndKeepsRawJson() throws Exception {
        answer(200, SANDBOX_BODY);

        MesriStudentStatus status = client.fetchStudentStatus("fc-token");

        assertThat(status.rawJson()).isEqualTo(SANDBOX_BODY);
        assertThat(status.sourceUrl()).isEqualTo("https://staging.particulier.api.gouv.fr/v3/mesri/statut_etudiant/france_connect");
        assertThat(status.recipientSiret()).isEqualTo("12006701200027");
        assertThat(status.identite()).isNull();
        assertThat(status.admissions()).hasSize(1);
        MesriStudentStatus.Admission admission = status.admissions().get(0);
        assertThat(admission.dateFin()).isEqualTo("2021-08-31");
        assertThat(admission.estInscrit()).isTrue();
        assertThat(admission.regimeCode()).isEqualTo("RF1");
        assertThat(admission.uai()).isEqualTo("0330023W");
        assertThat(admission.etablissementNom()).startsWith("LYCEE");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.uri().toString()).isEqualTo(
                "https://staging.particulier.api.gouv.fr/v3/mesri/statut_etudiant/france_connect?recipient=12006701200027");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer fc-token");
    }

    @Test
    void parsesTheIdentityBlockWhenPresent() throws Exception {
        answer(200, "{\"data\":{\"identite\":{\"nom_naissance\":\"Martin\",\"prenom\":\"Jacques\",\"date_naissance\":\"2004-01-01\"},"
                + "\"admissions\":[{\"date_debut\":\"2024-10-01\",\"date_fin\":\"2025-08-31\",\"est_inscrit\":true}]}}");

        MesriStudentStatus status = client.fetchStudentStatus("fc-token");

        assertThat(status.identite()).isEqualTo(new MesriStudentStatus.Identity("Martin", "Jacques", "2004-01-01"));
    }

    @Test
    void emptyAdmissionsIsAnError() throws Exception {
        answer(200, "{\"data\":{\"admissions\":[]},\"links\":{},\"meta\":{}}");

        assertThatThrownBy(() -> client.fetchStudentStatus("fc-token")).isInstanceOf(MesriNoAdmissionException.class);
    }

    @Test
    void unauthorizedMeansFranceConnectTokenProblem() throws Exception {
        answer(401, "{\"errors\":[{\"code\":\"51502\",\"title\":\"Accès non autorisé\",\"detail\":\"Le jeton d'accès n'a pas été trouvé ou est expiré.\"}]}");

        assertThatThrownBy(() -> client.fetchStudentStatus("fc-token"))
                .isInstanceOf(FranceConnectTokenUnavailableException.class)
                .hasMessageContaining("51502");
    }

    @Test
    void notFoundMeansUnknownStudent() throws Exception {
        answer(404, "{\"errors\":[{\"code\":\"25003\",\"title\":\"Entité non trouvée\"}]}");

        assertThatThrownBy(() -> client.fetchStudentStatus("fc-token"))
                .isInstanceOf(MesriStudentNotFoundException.class)
                .hasMessageContaining("25003");
    }

    @Test
    void serverErrorIsBadGateway() throws Exception {
        answer(500, "oops");

        assertThatThrownBy(() -> client.fetchStudentStatus("fc-token"))
                .isInstanceOf(ApiParticulierException.class)
                .hasMessageContaining("500");
    }

    @Test
    void missingConfigurationFailsBeforeAnyCall() {
        ApiParticulierClientImpl unconfigured = new ApiParticulierClientImpl("", "", httpClient, new ObjectMapper());

        assertThatThrownBy(() -> unconfigured.fetchStudentStatus("fc-token")).isInstanceOf(ApiParticulierException.class);
    }
}
