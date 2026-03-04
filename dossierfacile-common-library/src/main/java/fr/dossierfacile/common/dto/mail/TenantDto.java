package fr.dossierfacile.common.dto.mail;

import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TenantDto extends UserDto {
    private Long id;
    private List<UserApiDto> userApis;
    private TenantFileStatus status;
}
