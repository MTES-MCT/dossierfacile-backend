package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FranceIdentiteApiResultAttributes implements Serializable {
    String familyName;
    String givenName;
    String birthDate;
    String birthPlace;
    String validityDate;
}
