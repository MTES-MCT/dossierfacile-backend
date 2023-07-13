package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropertyLog {

    private static final long serialVersionUID = -3603846262626208324L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    private LocalDateTime creationDate;

    public PropertyLog(Property property) {
        this.property = property;
        creationDate = LocalDateTime.now();
    }

}
