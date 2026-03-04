package fr.dossierfacile.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserFeatureAssignmentId implements Serializable {

    @Serial
    private static final long serialVersionUID = -8034983423434991885L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "feature_key", length = 100)
    private String featureKey;
}

