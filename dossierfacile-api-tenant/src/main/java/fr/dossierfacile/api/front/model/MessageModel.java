package fr.dossierfacile.api.front.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageModel {
    private int id;
    private LocalDateTime creationDateTime;
    private String messageBody;
    @JsonIgnore
    private Long fromUser;
    @JsonIgnore
    private Long toUser;
    private TypeMessage typeMessage;
}
