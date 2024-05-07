package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.shared.AbstractAuditable;
import fr.dossierfacile.common.enums.FileStorageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

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

    @Column(
            name = "providers",
            columnDefinition = "character varying[]"
    )
    protected List<String> providers = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "encryption_key_id")
    protected EncryptionKey encryptionKey;

}
