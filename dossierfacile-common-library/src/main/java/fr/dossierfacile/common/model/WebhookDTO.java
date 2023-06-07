package fr.dossierfacile.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.entity.UserApi;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookDTO {
    @JsonIgnore
    UserApi userApi = null;
}
