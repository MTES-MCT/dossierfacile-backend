package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.ProcessingCapacity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProcessCapacitiesDTO {
    private List<ProcessingCapacity> list;
}
