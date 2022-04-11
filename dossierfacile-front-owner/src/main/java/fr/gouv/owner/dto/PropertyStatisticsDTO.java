package fr.gouv.owner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyStatisticsDTO {

    private Long created;

    private Long deleted;

    private Long visited;

    private Long subscribed;
}
