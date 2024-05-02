package fr.dossierfacile.api.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserApiDto {
    private String name2;
    private String logoUrl;
    private String completedUrl;
    private String welcomeUrl;

}