package fr.dossierfacile.api.pdfgenerator.util;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

@Data
@RequiredArgsConstructor
@Builder
@Getter
@Setter
public class PdfMergeModel {
    private final InputStream inputStream;
    private final String extension;
    @Builder.Default
    private final Boolean applyWatermark = true;
}
