package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import fr.dossierfacile.common.enums.MonFranceConnectValidationStatus;
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
import java.util.List;

@Data
@Entity
@Table(name = "monfranceconnect_validation_result")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class MonFranceConnectValidationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = File.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    private String qrCodeContent;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<String> apiResponse;

    @Enumerated(EnumType.STRING)
    private MonFranceConnectValidationStatus validationStatus;

}
