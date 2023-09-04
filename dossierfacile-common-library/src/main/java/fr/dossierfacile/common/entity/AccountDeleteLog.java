package fr.dossierfacile.common.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "account_delete_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AccountDeleteLog implements Serializable {

    private static final long serialVersionUID = -2160787806180393702L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDateTime deletionDate;

    @Column
    private Long userId;

    @Column(columnDefinition = "text")
    private String jsonProfileBeforeDeletion;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Object jsonProfile;
}
