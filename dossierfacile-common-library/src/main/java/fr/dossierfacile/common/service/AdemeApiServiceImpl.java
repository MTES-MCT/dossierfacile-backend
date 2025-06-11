package fr.dossierfacile.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.exceptions.AdemeApiBadRequestException;
import fr.dossierfacile.common.exceptions.AdemeApiInternalServerErrorException;
import fr.dossierfacile.common.exceptions.AdemeApiNotFoundException;
import fr.dossierfacile.common.exceptions.AdemeApiUnauthorizedException;
import fr.dossierfacile.common.mapper.AdemeApiResultModelToAdemeResultModelMapper;
import fr.dossierfacile.common.model.AdemeResultModel;
import fr.dossierfacile.common.model.ademe.AdemeApiResultModel;
import fr.dossierfacile.common.service.interfaces.AdemeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ConditionalOnProperty(name = "ademe.api.base.url")
@Slf4j
@Service
public class AdemeApiServiceImpl implements AdemeApiService {

    @Value("${ademe.api.base.url}")
    private String ademeApiBaseUrl;
    @Value("${ademe.api.client.id}")
    private String ademeApiClientId;
    @Value("${ademe.api.client.secret}")
    private String ademeApiClientSecret;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final AdemeApiResultModelToAdemeResultModelMapper mapper = new AdemeApiResultModelToAdemeResultModelMapper();

    AdemeApiServiceImpl(
            String ademeApiBaseUrl,
            String ademeApiClientId,
            String ademeApiClientSecret,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.ademeApiBaseUrl = ademeApiBaseUrl;
        this.ademeApiClientId = ademeApiClientId;
        this.ademeApiClientSecret = ademeApiClientSecret;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Autowired
    AdemeApiServiceImpl(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public AdemeResultModel getDpeDetails(String dpeNumber) throws AdemeApiInternalServerErrorException, AdemeApiBadRequestException, AdemeApiUnauthorizedException, AdemeApiNotFoundException, InterruptedException {
        String url = ademeApiBaseUrl + "/pub/dpe/" + dpeNumber;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("client_id", ademeApiClientId)
                .header("client_secret", ademeApiClientSecret)
                .GET()
                .build();

        HttpResponse<String> response = null;

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.error("ADEME API CLIENT ERROR: {}", e.getMessage());
            throw new AdemeApiInternalServerErrorException(e.getMessage());
        }

        if (response.statusCode() == 404) {
            log.error("ADEME API Not Found Error: status code {}, message {}", response.statusCode(), response.body());
            throw new AdemeApiNotFoundException(dpeNumber);
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            log.error("ADEME API Unauthorized Error: status code {}, message {}", response.statusCode(), response.body());
            throw new AdemeApiUnauthorizedException(response.statusCode(), response.body());
        }

        if (response.statusCode() == 400) {
            log.error("ADEME API Bad Request Error: status code {}, message {}", response.statusCode(), response.body());
            throw new AdemeApiBadRequestException(response.body());
        }

        if (response.statusCode() == 500) {
            log.error("ADEME API Internal Server Error: status code {}, message {}", response.statusCode(), response.body());
            throw new AdemeApiInternalServerErrorException(response.body());
        }

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            var ademeApiModel = objectMapper.readValue(response.body(), AdemeApiResultModel.class);
            return mapper.convert(ademeApiModel);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error("ADEME API Response Parsing Error: {}", e.getMessage());
            throw new AdemeApiInternalServerErrorException("Failed to parse ADEME API response");
        }
    }
}
