package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@TypeDefs({
        @TypeDef(
                name = "list-type",
                typeClass = ListArrayType.class
        )
})
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentDeniedReasons implements Serializable {

    private static final long serialVersionUID = -2813321453107893609L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Type(type = "list-type")
    @Column(
            name = "checked_options",
            columnDefinition = "character varying[]"
    )
    private List<String> checkedOptions = new ArrayList<>();

    private String comment;

    @OneToOne(targetEntity = Message.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;
}
