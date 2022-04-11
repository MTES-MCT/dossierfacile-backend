package fr.dossierfacile.api.pdfgenerator.model;

import lombok.Builder;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

@Getter
@Builder
public class PdfTemplateParameters {

    @Builder.Default
    public PDRectangle mediaBox = PDRectangle.A4;
    @Builder.Default
    public float compressionQuality = 0.9f;
    @Builder.Default
    public PageDimension maxPage = PageDimension.A4128;
}