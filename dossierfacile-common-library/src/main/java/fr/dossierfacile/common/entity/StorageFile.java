package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.shared.AbstractAuditable;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.model.S3Bucket;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@SuperBuilder
public class StorageFile extends AbstractAuditable<String, Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected String name;
    protected String path;
    protected String label;
    protected String contentType;
    protected Long size;
    protected String md5;
    @Enumerated(EnumType.STRING)
    protected FileStorageStatus status;
    @Column
    @Enumerated(EnumType.STRING)
    protected ObjectStorageProvider provider;

    @Enumerated(EnumType.STRING)
    protected S3Bucket bucket;

    @Column(
            name = "providers",
            columnDefinition = "character varying[]"
    )
    protected List<String> providers = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "encryption_key_id")
    protected EncryptionKey encryptionKey;

    public static String getWatermarkRawPath() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/raw/" + UUID.randomUUID();
    }

    public static String getWatermarkPdfPath() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/watermark/" + UUID.randomUUID();
    }

}
