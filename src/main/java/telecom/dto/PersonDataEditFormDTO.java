package telecom.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonDataEditFormDTO {

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