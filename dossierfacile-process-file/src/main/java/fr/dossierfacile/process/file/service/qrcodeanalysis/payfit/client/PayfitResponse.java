package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayfitResponse {

    private Content content;
    private String country;
    private String header;

    @Getter
    @NoArgsConstructor
    public static class Content {

        private List<Info> companyInfo;
        private List<Info> employeeInfo;

    }

    @Getter
    @NoArgsConstructor
    public static class Info {

        private String label;
        private String value;

    }

}