package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import fr.dossierfacile.common.entity.shared.AbstractAuditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
