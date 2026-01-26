package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.FileStorageStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

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

    @OneToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "storage_file_id")
    private StorageFile storageFile;

    private int numberOfPages;

    @OneToOne
    @JoinColumn(name = "preview_file_id")
    private StorageFile preview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @ToString.Exclude
    private Document document;

    @Builder.Default
    @Column(name = "creation_date")
    private Date creationDateTime = new Date();

    @Nullable
    @OneToOne(cascade = {CascadeType.REMOVE}, mappedBy = "file", fetch = FetchType.LAZY)
    private BarCodeFileAnalysis fileAnalysis;

    @Nullable
    @OneToOne(cascade = {CascadeType.REMOVE}, mappedBy= "file", fetch = FetchType.LAZY)
    private ParsedFileAnalysis parsedFileAnalysis;

    // We need to implement a cascade set null on delete !
    // Since Spring boot 3.5.8 and Hibernate 6.2.11, when a foreign key doesn't have a cascade on delete, but a sql set null action.
    // We need to manually set the relation to null before deleting the parent entity.
    // So we use a @PreRemove method to set the blurryFileAnalysis to null before deleting the document.
    // Otherwise, we get an exception transient object error.
    @Nullable
    @OneToOne(mappedBy = "file", fetch = FetchType.LAZY)
    private BlurryFileAnalysis blurryFileAnalysis;

    @Nullable
    @OneToOne(cascade = {CascadeType.REMOVE}, mappedBy = "file", fetch = FetchType.LAZY)
    private FileMetadata fileMetadata;

    @PreRemove
    void deleteCascade() {
        if (storageFile != null)
            storageFile.setStatus(FileStorageStatus.TO_DELETE);
        if (preview != null)
            preview.setStatus(FileStorageStatus.TO_DELETE);
        if (blurryFileAnalysis != null) {
            blurryFileAnalysis.setFile(null);
        }
    }

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
