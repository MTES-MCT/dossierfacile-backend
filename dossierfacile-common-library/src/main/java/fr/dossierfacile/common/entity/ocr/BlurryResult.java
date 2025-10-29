package fr.dossierfacile.common.entity.ocr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

public record BlurryResult(
        boolean isBlank,
        boolean isBlurry,
        // This will be removed in futur versions, it stay as optional for deprecation period
        @Deprecated
        Optional<Float> laplacianVariance,
        // This will be removed in futur versions, it stay as optional for deprecation period
        @Deprecated
        Optional<Boolean> isReadable,
        Optional<Float> ocrMeanScore,
        Optional<Integer> ocrTokens
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8347582394758234758L;
}