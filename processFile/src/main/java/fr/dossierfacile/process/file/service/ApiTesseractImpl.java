package fr.dossierfacile.process.file.service;

import com.google.gson.Gson;
import fr.dossierfacile.process.file.model.TesseractRequest;
import fr.dossierfacile.process.file.model.TesseractResponse;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class ApiTesseractImpl implements ApiTesseract {
    private final Gson gson;
    private final RestTemplate restTemplate;
    @Value("${tesseract.api.url}")
    private String url;
    @Value("${domain.url}")
    private String domainUrl;
    @Value("${tesseract.api.key}")
    private String tesseractApiKey;

    @Override
    public String apiTesseract(List<String> path, int[] pages, int dpi) {
        String result = "";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", tesseractApiKey);
            TesseractRequest tesseractRequest = new TesseractRequest(path, pages, dpi);
            HttpEntity<TesseractRequest> request =
                    new HttpEntity<>(tesseractRequest, headers);
            log.info(gson.toJson(request.getBody()));
            long time = System.currentTimeMillis();
            ResponseEntity<TesseractResponse> response = restTemplate.exchange(url + "/ocr", HttpMethod.POST, request, TesseractResponse.class);
            long milliseconds = System.currentTimeMillis() - time;
            log.info("Time call api tesseract /ocr " + milliseconds + " millisecond");
            if (response.getBody() != null) {
                result = String.join(" ", response.getBody().getOutput());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            log.error("Error in api tesseract");
            Sentry.capture(e);
        }
        return result;
    }
}
