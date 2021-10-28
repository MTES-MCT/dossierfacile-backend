package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.api.front.validator.anotation.Extension;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import fr.dossierfacile.api.front.validator.group.ApiPartner;
import fr.dossierfacile.api.front.validator.group.Dossier;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.List;

@Data
@Extension
public abstract class DocumentForm {

    @Null(groups = Dossier.class)
    @NotNull(groups = ApiPartner.class)
    private Long tenantId;

    @SizeFile(max = 10, typeDocumentValidation = TypeDocumentValidation.PER_FILE)
    private List<MultipartFile> documents = new ArrayList<>();
}
