package fr.dossierfacile.api.pdf.form;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class DocumentForm {
    private List<MultipartFile> files = new ArrayList<>();
    private String watermark;
}
