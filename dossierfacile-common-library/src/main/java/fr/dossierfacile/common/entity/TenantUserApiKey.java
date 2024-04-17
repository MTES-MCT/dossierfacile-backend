package fr.dossierfacile.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class TenantUserApiKey implements Serializable {

    private static final long serialVersionUID = -6995083055775152920L;

    @Column(name = "tenant_id")
    Long tenantId;

    @Column(name = "userapi_id")
    Long userApiId;
}