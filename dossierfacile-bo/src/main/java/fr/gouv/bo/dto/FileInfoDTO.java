package fr.gouv.bo.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "All details about file")
public class FileInfoDTO {
    @ApiModelProperty(notes = "name of file")
    private String filename;
    @ApiModelProperty(notes = "uri to download file")
    private String fileDownloadUri;
    @ApiModelProperty(notes = "type of file")
    private String fileType;
    @ApiModelProperty(notes = "size of file")
    private long fileSize;
}
