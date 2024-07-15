package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.FranceIdentiteApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import static fr.dossierfacile.common.enums.DocumentSubCategory.FRANCE_IDENTITE;

@Service
@Slf4j
@RequiredArgsConstructor
public class FranceIdentiteParser implements FileParser<FranceIdentiteApiResult> {
    private final RestTemplate restTemplate;
    private static final String CALL_BACK_RESPONSE = "France Identité callback responseStatus: {}";

    @Value("${france.identite.api.url:https://dossierfacile-france-identite-numerique-api.osc-secnum-fr1.scalingo.io/api/validation/v1/check-doc-valid?all-attributes=true}")
    private String urlCallback;

    @Override
    public FranceIdentiteApiResult parse(File file) {
        ResponseEntity<FranceIdentiteApiResult> response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            Resource resource = new FileSystemResource(file.getPath());
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("file", resource, MediaType.APPLICATION_PDF);

            HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(multipartBodyBuilder.build(), headers);
            response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, FranceIdentiteApiResult.class);
            log.info(CALL_BACK_RESPONSE, response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Unable to parse");
            throw new RuntimeException(e);
        }
        if ( HttpStatus.OK != response.getStatusCode() && HttpStatus.ACCEPTED != response.getStatusCode()) {
            log.error("Failure on France Identité check:" + urlCallback + "- Status:" + response.getStatusCode());
        }
        return response.getBody();
    }


    @Override
    public boolean shouldTryToApply(fr.dossierfacile.common.entity.File file) {
        return file.getDocument().getDocumentSubCategory() == FRANCE_IDENTITE;
    }
}