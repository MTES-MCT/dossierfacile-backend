package fr.dossierfacile.api.pdf.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUrlResponse {
    private String url;
}
