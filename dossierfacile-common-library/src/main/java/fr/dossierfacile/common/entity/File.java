package fr.dossierfacile.common.entity;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class File implements Serializable {

    @Serial
    private static final long serialVersionUID = -1328132958302637660L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    private String originalName;

    private Long size;

    private int numberOfPages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @ToString.Exclude
    private Document document;

    @Builder.Default
    @Column(name = "creation_date")
    private Date creationDateTime = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encryption_key_id", nullable = true)
    private EncryptionKey key;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        File file = (File) o;
        return id != null && Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
