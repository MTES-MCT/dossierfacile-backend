package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dossierfacile.common.exceptions.AdemeApiBadRequestException;
import fr.dossierfacile.common.exceptions.AdemeApiInternalServerErrorException;
import fr.dossierfacile.common.exceptions.AdemeApiNotFoundException;
import fr.dossierfacile.common.exceptions.AdemeApiUnauthorizedException;
import fr.dossierfacile.common.model.AdemeApiResultModel;
import fr.dossierfacile.common.utils.MapperUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

@Service("ademeApiService")
@Slf4j
public class AdemeApiServiceImpl {

    @Value("${ademe.api.base.url:default}")
    private String ademeApiBaseUrl;
    @Value("${ademe.api.client.id:default}")
    private String ademeApiClientId;
    @Value("${ademe.api.client.secret:default}")
    private String ademeApiClientSecret;

    private final ObjectMapper objectMapper = MapperUtil.newObjectMapper();

    public AdemeApiResultModel getDpeDetails(String dpeNumber) throws IOException, InterruptedException {
        String url = ademeApiBaseUrl + "/dpe/" + dpeNumber;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("client_id", ademeApiClientId)
            .header("client_secret", ademeApiClientSecret)
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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
        AdemeApiResultModel ademeApiResultModel = objectMapper.readValue(response.body(), AdemeApiResultModel.class);

        return ademeApiResultModel;
    }
}
