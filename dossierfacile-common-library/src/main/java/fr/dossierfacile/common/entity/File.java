package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.shared.StoredFile;
import fr.dossierfacile.common.utils.FileUtility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.annotation.Nullable;
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
public class File implements StoredFile, Serializable {

    @Serial
    private static final long serialVersionUID = -1328132958302637660L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    private String originalName;

    private Long size;

    private String contentType;

    private int numberOfPages;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "preview_file_id")
    private StorageFile preview;

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

    @Nullable
    @OneToOne(mappedBy = "file", fetch = FetchType.LAZY)
    private QrCodeFileAnalysis fileAnalysis;

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

    public String getComputedContentType() {
        if (contentType == null) return FileUtility.computeMediaType(this.getPath());
        return contentType;
    }

    @Override
    public EncryptionKey getEncryptionKey() {
        return getKey();
    }
}
