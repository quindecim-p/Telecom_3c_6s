package telecom.dto;

import lombok.*;
import telecom.entity.BillingAccount;
import telecom.entity.PersonData;
import telecom.entity.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileViewDTO {

    private User user;
    private PersonData personData;
    private BillingAccount billingAccount;

}