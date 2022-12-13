package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.api.front.validator.anotation.Extension;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Extension
public abstract class DocumentForm {

    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @SizeFile(max = 10, typeDocumentValidation = TypeDocumentValidation.PER_FILE)
    private List<MultipartFile> documents = new ArrayList<>();

    public Optional<Long> getOptionalTenantId() {
        return Optional.ofNullable(tenantId);
    }

}
