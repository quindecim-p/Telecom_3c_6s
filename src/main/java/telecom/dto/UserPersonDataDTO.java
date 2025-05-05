package telecom.dto;

import telecom.entity.PersonData;
import telecom.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPersonDataDTO {

    private User user;
    private PersonData personData;

}