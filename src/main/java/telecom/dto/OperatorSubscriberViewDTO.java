package telecom.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatorSubscriberViewDTO {

    private int userId;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Date birthDate;
    private BigDecimal balance;
    private int activeServiceCount;
    private BigDecimal totalMonthlyCost = BigDecimal.ZERO;
    private boolean isBanned;

}