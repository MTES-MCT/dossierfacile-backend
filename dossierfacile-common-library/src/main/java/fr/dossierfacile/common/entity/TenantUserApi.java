package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.ListStringConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
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
