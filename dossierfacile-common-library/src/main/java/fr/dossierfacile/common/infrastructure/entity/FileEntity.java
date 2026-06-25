package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "file")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "storage_file_id")
    private StorageFile storageFile;

    private int numberOfPages;

    @OneToOne
    @JoinColumn(name = "preview_file_id")
    private StorageFile preview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @Builder.Default
    @Column(name = "creation_date")
    private Date creationDateTime = new Date();

    // Garde fou pour ne pas laisser de storage file orphelin dans la db.
    @PreRemove
    void deleteCascade() {
        if (storageFile != null) {
            storageFile.setStatus(FileStorageStatus.TO_DELETE);
        }
        if (preview != null) {
            preview.setStatus(FileStorageStatus.TO_DELETE);
        }
    }
}
