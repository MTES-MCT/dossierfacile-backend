package fr.dossierfacile.process.file.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TesseractRequest {
    private String url;
    private int[] pages;
    private int dpi = 150;
}
