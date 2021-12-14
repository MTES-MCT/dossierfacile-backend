package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.FileActionUploadType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {

    @NotNull
    private MultipartFile file;

    @Min(1)
    @Max(10)
    private int number;

    @NotNull
    private FileActionUploadType fileActionUploadType;

}
