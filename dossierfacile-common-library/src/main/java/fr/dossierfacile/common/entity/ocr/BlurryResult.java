package fr.dossierfacile.common.entity.ocr;

public record BlurryResult(
        BlurryAlgorithmType algorithm,
        double score
){}