package telecom.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.dto.StatisticsDTO;
import telecom.entity.ConnectedService;
import telecom.enums.RoleType;
import telecom.enums.ServiceStatus;
import telecom.enums.TariffType;
import telecom.repository.ConnectedServiceRepository;
import telecom.repository.UserRepository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final UserRepository userRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @Autowired
    public StatisticsService(UserRepository userRepository,
                             ConnectedServiceRepository connectedServiceRepository) {
        this.userRepository = userRepository;
        this.connectedServiceRepository = connectedServiceRepository;
    }

    /**
     * Собирает всю статистику для отображения.
     */
    @Transactional(readOnly = true)
    public StatisticsDTO getStatistics() {
        StatisticsDTO dto = new StatisticsDTO();

        dto.setTotalUserCount(userRepository.count());
        dto.setUserCountsByRole(calculateUserCountsByRole());
        dto.setBannedUserCount(userRepository.countByIsBannedTrue());

        dto.setSubscriberUserCount(userRepository.countByRole(RoleType.SUBSCRIBER));

        dto.setTotalConnectedServicesCount(connectedServiceRepository.count());
        dto.setConnectedServicesCountByStatus(calculateServiceCountsByStatus());
        dto.setActiveServicesCountByType(calculateActiveServicesCountByType());

        dto.setPendingApplicationsCount(dto.getConnectedServicesCountByStatus().getOrDefault(ServiceStatus.PENDING, 0L));

        dto.setTotalMonthlyRevenue(calculateTotalMonthlyRevenue());

        return dto;
    }

    /**
     * Подсчитывает пользователей.
     */
    private Map<RoleType, Long> calculateUserCountsByRole() {
        List<Map<String, Object>> results = userRepository.countUsersByRole();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RoleType) row.get("roleType"),
                        row -> (Long) row.get("count"),
                        (v1, v2) -> v1,
                        () -> new EnumMap<>(RoleType.class)
                ));
    }

    /**
     * Подсчитывает услуги по статусам.
     */
    private Map<ServiceStatus, Long> calculateServiceCountsByStatus() {
        List<Map<String, Object>> results = connectedServiceRepository.countServicesByStatus();
        Map<ServiceStatus, Long> counts = results.stream()
                .collect(Collectors.toMap(
                        row -> (ServiceStatus) row.get("serviceStatus"),
                        row -> (Long) row.get("count"),
                        (v1, v2) -> v1,
                        () -> new EnumMap<>(ServiceStatus.class)
                ));
        for (ServiceStatus status : ServiceStatus.values()) {
            counts.putIfAbsent(status, 0L);
        }
        return counts;
    }

    /**
     * Подсчитывает АКТИВНЫЕ услуги по типам.
     */
    private Map<TariffType, Long> calculateActiveServicesCountByType() {
        List<Map<String, Object>> results = connectedServiceRepository.countActiveServicesByType(ServiceStatus.ACTIVE);
        Map<TariffType, Long> counts = results.stream()
                .filter(row -> row.get("tariffType") != null)
                .collect(Collectors.toMap(
                        row -> (TariffType) row.get("tariffType"),
                        row -> (Long) row.get("count"),
                        (v1, v2) -> v1,
                        () -> new EnumMap<>(TariffType.class)
                ));
        for (TariffType type : TariffType.values()) {
            counts.putIfAbsent(type, 0L);
        }
        return counts;
    }

    /**
     * Подсчитывает общую выручку.
     */
    private BigDecimal calculateTotalMonthlyRevenue() {
        List<ConnectedService> activeServices = connectedServiceRepository.findByStatus(ServiceStatus.ACTIVE);
        if (activeServices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return activeServices.stream()
                .map(service -> service.getTariffPlan() != null ? service.getTariffPlan().getMonthlyPayment() : BigDecimal.ZERO)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}