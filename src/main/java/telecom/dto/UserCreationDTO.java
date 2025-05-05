package telecom.dto;

import lombok.AllArgsConstructor;
import telecom.enums.RoleType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreationDTO {

    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 4, max = 20, message = "Логин должен быть от 4 до 20 символов")
    private String login;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    private String password;

    @NotBlank(message = "Подтверждение пароля не может быть пустым")
    private String confirmPassword;

    @NotNull(message = "Нужно указать роль пользователя")
    private RoleType role;

    @NotBlank(message = "ФИО не может быть пустым")
    @Size(max = 50)
    private String fullName;

    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+375\\d{9}$", message = "Телефон должен быть в формате +375XXXXXXXXX")
    private String phone;

    @NotBlank(message = "Адрес почты не может быть пустым")
    @Pattern(
            regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$",
            message = "Введите корректный email"
    )
    @Size(max = 30)
    private String email;

    @NotBlank(message = "Адрес проживания не может быть пустым")
    @Size(max = 50)
    private String address;

    @NotNull(message = "Дата рождения не может быть пустой")
    @Past(message = "Дата рождения должна быть в прошлом")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;

}