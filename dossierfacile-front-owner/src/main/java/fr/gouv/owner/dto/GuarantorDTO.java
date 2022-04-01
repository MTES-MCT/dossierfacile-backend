package fr.gouv.owner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.entity.Guarantor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "All details about guarantor")
public class GuarantorDTO {

    @NotEmpty
    @Size(min = 3, max = 50)
    @ApiModelProperty(notes = "first name of guarantor")
    private String firstName;

    @NotEmpty
    @Size(min = 3, max = 50)
    @ApiModelProperty(notes = "last name of guarantor")
    private String lastName;

    @JsonIgnore
    private MultipartFile[][] files;
    @JsonIgnore
    private String[] path;
    @JsonIgnore
    private String file4Text;

    public GuarantorDTO(Guarantor guarantor) {
    }
}
