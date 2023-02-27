package fr.dossierfacile.process.file.service.monfranceconnect.client;

import fr.dossierfacile.process.file.util.QrCode;
import lombok.Builder;
import org.apache.http.NameValuePair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.http.client.utils.URLEncodedUtils.parse;

@Builder
public class DocumentVerificationRequest {

    private final String documentId;
    private final String documentData;

    static DocumentVerificationRequest forDocumentWith(QrCode qrCode) {
        URI uri = URI.create(qrCode.getContent());
        List<NameValuePair> params = parse(uri, StandardCharsets.UTF_8);
        DocumentVerificationRequestBuilder builder = DocumentVerificationRequest.builder();
        for (NameValuePair param : params) {
            switch (param.getName()) {
                case "id":
                    builder.documentId(param.getValue());
                case "data":
                    builder.documentData(param.getValue());
            }
        }
        return builder.build();
    }

    URI getUri(String host) {
        return UriComponentsBuilder.fromUriString(host)
                .path("/verif-justificatif")
                .queryParam("idJustificatif", documentId)
                .build().toUri();
    }

    HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(documentData, headers);
    }

}
