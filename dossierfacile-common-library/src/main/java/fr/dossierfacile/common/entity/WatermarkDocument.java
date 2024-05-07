package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.FileStorageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "watermark_document")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WatermarkDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nullable
    @CreatedDate
    private LocalDateTime createdDate;

    private String token;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @JoinTable(
            name = "watermark_document_storage_file",
            joinColumns = @JoinColumn(name = "watermark_document_id"),
            inverseJoinColumns = @JoinColumn(name = "storage_file_id")
    )
    private List<StorageFile> files = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private FileStatus pdfStatus;

    @OneToOne
    @JoinColumn(name = "pdf_file_id")
    private StorageFile pdfFile;

    private String text;

    @PreRemove
    void deleteCascade() {
        if (pdfFile != null)
            pdfFile.setStatus(FileStorageStatus.TO_DELETE);
    }
}
