package fr.gouv.bo.model.owner;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.bo.model.UserModel;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerModel extends UserModel {

    private List<PropertyModel> properties;

    private String slug;

    private StepRegisterOwner stepRegisterOwner;
}