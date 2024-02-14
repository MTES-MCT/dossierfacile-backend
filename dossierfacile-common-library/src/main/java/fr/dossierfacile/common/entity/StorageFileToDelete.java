package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import fr.dossierfacile.common.entity.shared.AbstractAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@TypeDefs({
        @TypeDef(
                name = "list-type",
                typeClass = ListArrayType.class
        )
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@SuperBuilder
public class StorageFileToDelete extends AbstractAuditable<String, Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected String path;

    @Type(type = "list-type")
    @Column(
            name = "providers",
            columnDefinition = "character varying[]"
    )
    protected List<String> providers = new ArrayList<>();

}
