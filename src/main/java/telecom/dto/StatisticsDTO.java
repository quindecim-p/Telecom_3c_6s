package telecom.dto;

import lombok.*;
import telecom.enums.RoleType;
import telecom.enums.ServiceStatus;
import telecom.enums.TariffType;

import java.math.BigDecimal;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class StatisticsDTO {

    private long totalUserCount;
    private Map<RoleType, Long> userCountsByRole;
    private long bannedUserCount;
    private long subscriberUserCount;

    private long totalConnectedServicesCount;
    private Map<ServiceStatus, Long> connectedServicesCountByStatus;
    private Map<TariffType, Long> activeServicesCountByType;
    private long pendingApplicationsCount;

    private BigDecimal totalMonthlyRevenue;

}