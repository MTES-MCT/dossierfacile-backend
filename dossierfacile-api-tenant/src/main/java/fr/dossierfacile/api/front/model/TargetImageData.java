package fr.dossierfacile.api.front.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TargetImageData {
    float targetWidth = 0;
    float targetHeight = 0;
    float horizontalDisplacement = 0;
    float verticalDisplacement = 0;
}
