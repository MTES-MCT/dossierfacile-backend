package fr.gouv.bo.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileModel {
    private Long id;
    private String path;
    private String originalName;
    private int numberOfPages;
    private Long size;
    private String contentType;
}
