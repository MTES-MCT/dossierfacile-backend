package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @SizeFile(max = 10)
    private List<MultipartFile> documents = new ArrayList<>();

}
