package fr.dossierfacile.common.dto.mail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String firstName;
    private String lastName;
    private String preferredName;
    private String email;
    private String keycloakId;

    public String getFullName() {
        String displayName = isBlank(preferredName) ? lastName : preferredName;
        return isNotBlank(firstName) && isNotBlank(displayName) ? String.join(" ", firstName, displayName) : "";
    }
}