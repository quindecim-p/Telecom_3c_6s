package telecom.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "persondata")
public class PersonData implements Serializable {

    @Id
    @Column(name = "user_id")
    private int userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "ФИО не может быть пустым")
    @Column(length = 50, name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Телефон не может быть пустым")
    @Column(length = 13, nullable = false, unique = true)
    private String phone;

    @NotBlank(message = "Адрес почты не может быть пустым")
    @Column(length = 30, nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Адрес проживания не может быть пустым")
    @Column(length = 50, nullable = false)
    private String address;

    @NotNull(message = "Дата рождения не может быть пустой")
    @Column(name = "birth_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    public PersonData(String fullName, String phone, String email, String address, Date birthDate, User user) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.birthDate = birthDate;
        this.user = user;
    }

}
