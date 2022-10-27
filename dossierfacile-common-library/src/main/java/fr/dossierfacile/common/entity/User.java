package fr.dossierfacile.common.entity;

import com.google.common.base.Strings;
import fr.dossierfacile.common.enums.AuthProvider;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import fr.dossierfacile.common.enums.UserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_account")
@Inheritance(
        strategy = InheritanceType.JOINED
)
@Getter
@Setter
@NoArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -3603815439883206021L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String preferredName;

    @Column
    private String email;

    @Column
    private String password;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<UserRole> userRoles = new HashSet<>();

    @Column(name = "creation_date")
    private LocalDateTime creationDateTime = LocalDateTime.now();

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private Date updateDateTime;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate = LocalDateTime.now();

    @Column(columnDefinition = "boolean default true")
    private boolean enabled = false;

    @OneToMany(mappedBy = "fromUser", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Message> sentMessages = new HashSet<>();
    @OneToMany(mappedBy = "toUser", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Message> receivedMessages = new HashSet<>();

    @Column(length = 20, columnDefinition = "varchar(20) default 'local'")
    @Enumerated(EnumType.STRING)
    private AuthProvider provider = AuthProvider.local;
    private String providerId;
    private String imageUrl;

    private String keycloakId;
    private Boolean franceConnect = false;
    private String franceConnectSub;
    private String franceConnectBirthDate;
    private String franceConnectBirthPlace;
    private String franceConnectBirthCountry;

    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private ConfirmationToken confirmationToken;

    public User(UserType userType, String email) {
        this.userType = userType;
        this.email = email;
    }

    public User(UserType userType, String firstName, String lastName, String email) {
        this.userType = userType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getFullName() {
        String displayName = Strings.isNullOrEmpty(preferredName) ? lastName : preferredName;
        return firstName != null && displayName != null ? String.join(" ", firstName, displayName) : "";
    }
}
