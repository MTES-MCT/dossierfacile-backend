package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocHeader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class AntsTrustServiceList {

    private final Map<String, URI> uriByIssuers = new HashMap<>();
    private final String tslUri;

    public AntsTrustServiceList(@Value("${ants.tsl.uri}") String tslUri) {
        this.tslUri = tslUri;
    }

    @PostConstruct
    void downloadAndParseList() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(tslUri);

        NodeList trustServiceProviders = document.getElementsByTagName("tsl:TrustServiceProvider");

        for (int i = 0; i < trustServiceProviders.getLength(); i++) {
            Element provider = (Element) trustServiceProviders.item(i);

            Element nameElement = (Element) provider.getElementsByTagName("tsl:TSPTradeName").item(0);
            String name = nameElement.getElementsByTagName("tsl:Name").item(0).getTextContent();

            Element uriElement = (Element) provider.getElementsByTagName("tsl:TSPInformationURI").item(0);
            String uri = uriElement.getElementsByTagName("tsl:URI").item(0).getTextContent();

            uriByIssuers.put(name, URI.create(uri));
        }
    }

    public URI getCertificateUri(TwoDDocHeader twoDDocHeader) {
        String issuer = twoDDocHeader.issuer();
        if (!uriByIssuers.containsKey(issuer)) {
            throw new TwoDDocValidationException("Unsupported certification authority: " + issuer);
        }
        return UriComponentsBuilder.fromUri(uriByIssuers.get(issuer))
                .queryParam("name", twoDDocHeader.certId())
                .build().toUri();
    }

}
