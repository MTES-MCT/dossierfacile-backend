package fr.dossierfacile.api.front.form;

import lombok.Builder;
import org.springframework.util.MultiValueMap;

@Builder
public record AcquisitionData(String campaign, String source, String medium) {

    public static AcquisitionData from(MultiValueMap<String, String> params) {
        return AcquisitionData.builder()
                .campaign(params.getFirst("campaign"))
                .source(params.getFirst("source"))
                .medium(params.getFirst("medium"))
                .build();
    }

}
