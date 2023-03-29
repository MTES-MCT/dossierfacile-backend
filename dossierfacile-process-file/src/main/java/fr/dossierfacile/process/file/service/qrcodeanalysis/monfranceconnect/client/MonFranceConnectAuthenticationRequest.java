package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client;

import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationRequest;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.Builder;
import org.apache.http.NameValuePair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.http.client.utils.URLEncodedUtils.parse;

@Builder
public class MonFranceConnectAuthenticationRequest implements AuthenticationRequest {

    private static final String PATH = "/verif-justificatif";
    private static final String ID_PARAM = "id";
    private static final String DATA_PARAM = "data";

    private final String documentId;
    private final String documentData;

    public static Optional<MonFranceConnectAuthenticationRequest> forDocumentWith(QrCode qrCode) {
        URI uri = URI.create(qrCode.getContent());
        Map<String, String> queryParams = parse(uri, StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        if (isMonFranceConnectUrl(uri, queryParams)) {
            var request = new MonFranceConnectAuthenticationRequest(queryParams.get("id"), queryParams.get("data"));
            return Optional.of(request);
        }
        return Optional.empty();
    }

    private static boolean isMonFranceConnectUrl(URI uri, Map<String, String> queryParams) {
        return PATH.equals(uri.getPath()) &&
                queryParams.containsKey(ID_PARAM) &&
                queryParams.containsKey(DATA_PARAM);
    }

    URI getUri(String host) {
        return UriComponentsBuilder.fromUriString(host)
                .path(PATH)
                .queryParam("idJustificatif", documentId)
                .build().toUri();
    }

    HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(documentData, headers);
    }

}
