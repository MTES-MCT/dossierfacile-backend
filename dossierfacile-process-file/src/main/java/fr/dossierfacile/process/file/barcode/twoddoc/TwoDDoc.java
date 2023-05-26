package fr.dossierfacile.process.file.barcode.twoddoc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class TwoDDoc {

    private String iDFlag;
    private int version;
    private String issuer;
    private String certId;
    private String documentDate;
    private String signatureDate;
    private String documentTypeId;
    private String perimeterId;
    private String countryId;

    private HashMap<String, String> data = new HashMap<>();

    public String getFiscalNumber() {
        return data.get("47");
    }

    public String getReferenceNumber() {
        return data.get("42");
    }

    private String signature;
}
