package fr.dossierfacile.common.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.enums.TypeUserApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_api")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserApi implements Serializable {

    private static final long serialVersionUID = 8225702491583459392L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String urlCallback;

    @Column
    private String name;

    @Column
    private String name2;

    @Column
    private String site;

    @Column
    private String apiKey;

    @Column
    private TypeUserApi typeUserApi;

    private String textModal;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean logo = false;

    private String partnerApiKeyCallback;

    private Integer version;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "userApi", fetch = FetchType.LAZY)
    private List<TenantUserApi> tenantsUserApi = new ArrayList<>();

    public UserApi(String name, String url, String site, String name2, String textModal) {
        this.name = name;
        this.urlCallback = url;
        this.site = site;
        this.name2 = name2;
        this.textModal = textModal;
        this.version = 1;
    }

    @Override
    public String toString() {
        return "user_api: " + id;
    }
}
