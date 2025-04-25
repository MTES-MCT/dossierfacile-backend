package fr.dossierfacile.common.entity.ocr;

import java.io.Serial;
import java.io.Serializable;

public record BlurryResult(
        BlurryAlgorithmType algorithm,
        double score
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8347582394758234758L;
}