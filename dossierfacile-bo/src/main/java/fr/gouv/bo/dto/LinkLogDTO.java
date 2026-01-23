package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkLogDTO {

    private Long id;
    private LinkType linkType;
    private LocalDateTime creationDate;
    private String ipAddress;

    public static LinkLogDTO fromEntity(LinkLog log) {
        return LinkLogDTO.builder()
                .id(log.getId())
                .linkType(log.getLinkType())
                .creationDate(log.getCreationDate())
                .ipAddress(obfuscateIpAddress(log.getIpAddress()))
                .build();
    }

    private static String obfuscateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return null;
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            // IPv4: keep first and last octet, obfuscate middle ones (e.g., 54.xxx.xxx.172)
            return parts[0] + ".xxx.xxx." + parts[3];
        }

        // For IPv6 or other formats, obfuscate most of it
        if (ipAddress.length() > 8) {
            return ipAddress.substring(0, 4) + "xxx" + ipAddress.substring(ipAddress.length() - 4);
        }

        return "xxx";
    }
}
