package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "qrcode_file_analysis")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class QrCodeFileAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Enumerated(EnumType.STRING)
    private DocumentIssuer issuerName;

    private String qrCodeContent;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Object apiResponse;

    @Enumerated(EnumType.STRING)
    private FileAuthenticationStatus authenticationStatus;

    private boolean allowedInDocumentCategory;

}
