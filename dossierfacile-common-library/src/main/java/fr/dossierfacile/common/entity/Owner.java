package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.dossierfacile.common.enums.UserType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owner")
@DiscriminatorValue("OWNER")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "lombokBuilder")
public class Owner extends User implements Serializable {

    private static final long serialVersionUID = -4711959104392579912L;

    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Property> properties = new ArrayList<>();

    @Builder.Default
    private String slug = RandomStringUtils.randomAlphanumeric(30);

    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition = "int4")
    private StepRegisterOwner stepRegisterOwner;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean example = true;

    public static Owner.OwnerBuilder<?, ?> builder() {
        Owner.OwnerBuilder<?, ?> builder = Owner.lombokBuilder();
        builder.userType(UserType.OWNER);
        return builder;
    }

}
