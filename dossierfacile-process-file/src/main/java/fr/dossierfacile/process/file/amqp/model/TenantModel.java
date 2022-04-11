package fr.dossierfacile.process.file.amqp.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantModel {
    Long id;
}
