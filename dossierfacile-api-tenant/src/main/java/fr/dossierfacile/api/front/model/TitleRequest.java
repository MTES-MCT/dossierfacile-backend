package fr.dossierfacile.api.front.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TitleRequest {
    @NotBlank(message = "Title is required and cannot be blank")
    private String title;
}
