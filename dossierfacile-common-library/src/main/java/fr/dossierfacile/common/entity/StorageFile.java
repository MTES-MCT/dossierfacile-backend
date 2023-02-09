package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.entity.shared.AbstractAuditable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.io.Serial;

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
    @Column
    @Enumerated(EnumType.STRING)
    protected ObjectStorageProvider provider;
    @ManyToOne
    @JoinColumn(name = "encryption_key_id")
    protected EncryptionKey encryptionKey;

}