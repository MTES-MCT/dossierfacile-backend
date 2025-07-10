package fr.dossierfacile.common.entity.ocr;

import java.io.Serial;
import java.io.Serializable;

public record BlurryResult(
        boolean isBlank,
        boolean isBlurry,
        float laplacianVariance
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8347582394758234758L;
}