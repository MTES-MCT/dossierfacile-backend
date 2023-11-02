package fr.dossierfacile.api.dossierfacileapiowner.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import fr.dossierfacile.api.dossierfacileapiowner.property.PropertyModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class DeletedOwnerModel {
    private Long id;
    private String hEmail;
    private String hFirstName;
    private String hLastName;
    private String hPreferredName;
    private boolean franceConnect;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime creationDateTime;
    private List<PropertyModel> properties;
}
