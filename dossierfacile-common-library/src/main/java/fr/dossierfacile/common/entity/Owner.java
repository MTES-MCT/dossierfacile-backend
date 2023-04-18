package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.dossierfacile.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.RandomStringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
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

    private StepRegisterOwner stepRegisterOwner;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean example = true;

    public static Owner.OwnerBuilder<?, ?> builder() {
        Owner.OwnerBuilder<?, ?> builder = Owner.lombokBuilder();
        builder.userType(UserType.OWNER);
        return builder;
    }

    public Property lastProperty() {
        properties.sort(Comparator.comparing(Property::getCreationDateTime).reversed());
        if (!properties.isEmpty()) {
            return properties.get(0);
        } else {
            return null;
        }
    }
}
