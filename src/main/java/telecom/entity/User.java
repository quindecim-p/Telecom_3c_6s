package telecom.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import telecom.enums.RoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "user")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Setter
    @NotBlank(message = "Логин не может быть пустым")
    @Column(nullable = false, unique = true)
    private String login;

    @Getter
    @NotBlank(message = "Пароль не может быть пустым")
    @Column(nullable = false)
    private String password;

    @NotNull(message = "Тип роли не может быть пустым")
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType role;

    @Column(nullable = false)
    private boolean isBanned;

    @OneToOne(mappedBy = "user",
            optional = false)
    private PersonData personData;

    public User(String login, String password, RoleType role, boolean isBanned) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.isBanned = isBanned;
    }

}