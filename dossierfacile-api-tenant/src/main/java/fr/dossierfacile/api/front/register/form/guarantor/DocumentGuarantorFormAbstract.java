package fr.dossierfacile.api.front.register.form.guarantor;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.anotation.guarantor.NumberOfDocumentGuarantor;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.ExistGuarantor;
import fr.dossierfacile.api.front.validator.group.DocumentIdentificationGuarantor;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ExistGuarantor
@NumberOfDocumentGuarantor(max = 5, groups = DocumentIdentificationGuarantor.class)
public abstract class DocumentGuarantorFormAbstract extends DocumentForm {

    @NotNull
    private Long guarantorId;

    private TypeGuarantor typeGuarantor;

    private DocumentCategory documentCategory;
}
