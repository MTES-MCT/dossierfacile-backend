package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * OWASP File Upload — "Set a file size limit".
 * MIME type validation is performed explicitly in AbstractDocumentSaveStep after detection via Tika.
 */
@Data
public abstract class DocumentForm implements FormWithTenantId {

    private Long tenantId;

    @SizeFile(max = 10)
    private List<MultipartFile> documents = new ArrayList<>();

}
