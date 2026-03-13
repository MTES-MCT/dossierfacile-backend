package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * OWASP File Upload — "List allowed extensions" / "Validate the file type, don't trust the Content-Type header".
 */
@Data
public abstract class DocumentForm implements FormWithTenantId {

    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @AllowedMimeTypes({"application/pdf", "image/jpeg", "image/png", "image/heif"})
    @SizeFile(max = 10)
    private List<MultipartFile> documents = new ArrayList<>();

}
