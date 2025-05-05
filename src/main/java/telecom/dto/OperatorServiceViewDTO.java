package telecom.dto;

import lombok.*;
import telecom.enums.ServiceStatus;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatorServiceViewDTO {

    private int serviceId;
    private String userFullName;
    private String userPhone;
    private String tariffName;
    private LocalDate startDate;
    private ServiceStatus status;

}