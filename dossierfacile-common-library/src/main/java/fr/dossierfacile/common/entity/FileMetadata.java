package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.StringMapJsonbConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "file_metadata")
public class FileMetadata implements Serializable {

    @Serial
    private static final long serialVersionUID = -1328132958302637660L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "metadata")
    @Convert(converter = StringMapJsonbConverter.class)
    private Map<String, String> metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FileMetadata that = (FileMetadata) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", fileId=" + (file != null ? file.getId() : null) +
                ", metadata=" + metadata +
                '}';
    }
}
