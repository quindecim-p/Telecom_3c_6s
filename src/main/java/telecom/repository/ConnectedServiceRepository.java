package telecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import telecom.entity.ConnectedService;
import telecom.entity.Subscriber;
import telecom.entity.TariffPlan;
import telecom.enums.ServiceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ConnectedServiceRepository extends JpaRepository<ConnectedService, Integer> {

    List<ConnectedService> findBySubscriberAndStatus(Subscriber subscriber, ServiceStatus status);

    List<ConnectedService> findBySubscriber(Subscriber subscriber);

    List<ConnectedService> findByStatusOrderByIdAsc(ServiceStatus status);

    boolean existsBySubscriberAndTariffPlanAndStatusIn(Subscriber subscriber, TariffPlan tariffPlan, List<ServiceStatus> statuses);

    List<ConnectedService> findBySubscriberAndStatusIn(Subscriber subscriber, List<ServiceStatus> statuses);

    List<ConnectedService> findBySubscriberOrderByStatusAscStartDateDesc(Subscriber subscriber);

    long countByTariffPlan(TariffPlan tariffPlan);

    List<ConnectedService> findByStatus(ServiceStatus status);

    List<ConnectedService> findBySubscriberInAndStatus(List<Subscriber> subscribers, ServiceStatus status);

    @Query("SELECT cs.status as serviceStatus, COUNT(cs) as count FROM ConnectedService cs GROUP BY cs.status")
    List<Map<String, Object>> countServicesByStatus();

    @Query("SELECT cs.tariffPlan.type as tariffType, COUNT(cs) as count " +
            "FROM ConnectedService cs " +
            "WHERE cs.status = :status " +
            "GROUP BY cs.tariffPlan.type")
    List<Map<String, Object>> countActiveServicesByType(@Param("status") ServiceStatus status);

    List<ConnectedService> findByStatusAndNextBillingDateLessThanEqual(ServiceStatus status, LocalDate today);

}