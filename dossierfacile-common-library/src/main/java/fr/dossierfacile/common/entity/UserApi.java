package fr.dossierfacile.common.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String partnerApiKeyCallback;
    @Column
    private Integer version;
    @Column
    private String logoUrl;
    @Column
    private String welcomeUrl;
    @Column
    private String completedUrl;
    @Column
    private String deniedUrl;
    @Column
    private String validatedUrl;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "userApi", fetch = FetchType.LAZY)
    private List<TenantUserApi> tenantsUserApi = new ArrayList<>();
    @Column
    private boolean disabled;

    @Override
    public String toString() {
        return "user_api: " + id;
    }
}
