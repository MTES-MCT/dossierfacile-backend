package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * HTTP client for the API Particulier "statut étudiant" endpoint, FranceConnect mode.
 * Modelled on {@code AdemeApiServiceImpl}: Java HttpClient + Jackson, package-private constructor for tests.
 */
@Slf4j
@Service
public class ApiParticulierClientImpl implements ApiParticulierClient {

    static final String STUDENT_STATUS_PATH = "/v3/mesri/statut_etudiant/france_connect";

    @Value("${api-particulier.base-url:}")
    private String baseUrl;
    @Value("${api-particulier.recipient-siret:}")
    private String recipientSiret;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    ApiParticulierClientImpl(String baseUrl, String recipientSiret, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.recipientSiret = recipientSiret;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Autowired
    ApiParticulierClientImpl(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public MesriStudentStatus fetchStudentStatus(String franceConnectAccessToken) {
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(recipientSiret)) {
            throw new ApiParticulierException("API Particulier is not configured (api-particulier.base-url / recipient-siret)");
        }
        String sourceUrl = baseUrl + STUDENT_STATUS_PATH;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sourceUrl + "?recipient=" + recipientSiret))
                .header("Authorization", "Bearer " + franceConnectAccessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.error("API Particulier network error: {}", e.getMessage());
            throw new ApiParticulierException("API Particulier unreachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiParticulierException("Interrupted while calling API Particulier");
        }

        int status = response.statusCode();
        String body = response.body();
        switch (status) {
            case 200 -> {
                return parse(body, sourceUrl);
            }
            case 401 -> throw new FranceConnectTokenUnavailableException(
                    "API Particulier rejected the FranceConnect token: " + errorSummary(body));
            case 404 -> throw new MesriStudentNotFoundException("No student found by the MESR: " + errorSummary(body));
            default -> {
                log.error("API Particulier answered {}: {}", status, StringUtils.abbreviate(body, 500));
                throw new ApiParticulierException("API Particulier answered HTTP " + status + ": " + errorSummary(body));
            }
        }
    }

    private MesriStudentStatus parse(String body, String sourceUrl) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            throw new ApiParticulierException("API Particulier returned an unreadable body");
        }
        List<MesriStudentStatus.Admission> admissions = MesriStudentStatus.parseAdmissions(root);
        if (admissions.isEmpty()) {
            throw new MesriNoAdmissionException();
        }
        return new MesriStudentStatus(body, sourceUrl, recipientSiret, admissions);
    }

    /** Extracts "code title" from the API Particulier error envelope {@code {"errors":[{"code","title",...}]}}. */
    private String errorSummary(String body) {
        try {
            JsonNode first = objectMapper.readTree(body).path("errors").path(0);
            if (first.isMissingNode()) {
                return StringUtils.abbreviate(body, 120);
            }
            return first.path("code").asText("") + " " + first.path("title").asText("");
        } catch (IOException e) {
            return StringUtils.abbreviate(body, 120);
        }
    }
}
