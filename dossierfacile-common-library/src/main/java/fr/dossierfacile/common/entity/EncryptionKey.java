package fr.dossierfacile.common.entity;

import lombok.*;

import javax.persistence.*;
import java.security.Key;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
public class EncryptionKey implements Key {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EncryptionKeyStatus status;

    private String algorithm;

    private String format;

    @Column(name = "encoded", length = 128)
    private byte[] encodedSecret;

    private int version;

    public byte[] getEncoded() {
        return encodedSecret.clone();
    }
}
