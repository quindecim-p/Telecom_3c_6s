package telecom.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountEditFormDTO {

    @Size(min = 4, max = 20, message = "Логин должен быть от 4 до 20 символов")
    private String login;

    private String currentPassword;

    private String newPassword;

    private String confirmNewPassword;

}