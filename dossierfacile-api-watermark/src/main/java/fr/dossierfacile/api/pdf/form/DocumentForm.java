package fr.dossierfacile.api.pdf.form;

import fr.dossierfacile.common.validator.annotation.SizeFile;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * OWASP File Upload — "Set a file size limit" / "Set a filename length limit".
 * MIME type validation is performed explicitly in DocumentServiceImpl after detection via Tika.
 */
@Data
public class DocumentForm {

    @NotEmpty(message = "you must add some file")
    @SizeFile(max = 20)
    private List<MultipartFile> files = new ArrayList<>();

    @Size(max = 100, message = "Le filigrane ne peut dépasser 100 caractères")
    private String watermark;
}
