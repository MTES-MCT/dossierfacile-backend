package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.ListStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenant_userapi")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantUserApi implements Serializable {

    private static final long serialVersionUID = -3404347613657138333L;

    @EmbeddedId
    TenantUserApiKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tenantId")
    @JoinColumn(name = "tenant_id")
    Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userApiId")
    @JoinColumn(name = "userapi_id")
    UserApi userApi;

    @Builder.Default
    @Column(columnDefinition = "text")
    @Convert(converter = ListStringConverter.class)
    private List<String> allInternalPartnerId = new ArrayList<>();

    @Builder.Default
    private LocalDateTime accessGrantedDate = LocalDateTime.now();

}
