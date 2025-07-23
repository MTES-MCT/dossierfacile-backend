package fr.dossierfacile.api.front.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullPdfFile {
    private ByteArrayOutputStream fileOutputStream;
    private String fileName;
}