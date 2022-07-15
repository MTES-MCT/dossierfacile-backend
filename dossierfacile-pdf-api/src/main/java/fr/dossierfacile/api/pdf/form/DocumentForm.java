package fr.dossierfacile.api.pdf.form;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.web.multipart.MultipartFile;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentForm {

    private Long documentId;

    private List<MultipartFile> files = new ArrayList<>();
}
