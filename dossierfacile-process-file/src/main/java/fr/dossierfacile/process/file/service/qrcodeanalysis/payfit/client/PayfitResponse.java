package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client;


import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
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
        private int order;
        private String value;

    }

}