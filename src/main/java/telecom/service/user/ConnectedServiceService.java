package telecom.service.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.entity.*;
import telecom.enums.ServiceStatus;
import telecom.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConnectedServiceService {

    private final ConnectedServiceRepository connectedServiceRepository;
    private final TariffPlanRepository tariffPlanRepository;
    private final SubscriberRepository subscriberRepository;
    private final UserRepository userRepository;

    @Autowired
    public ConnectedServiceService(ConnectedServiceRepository connectedServiceRepository,
                                   TariffPlanRepository tariffPlanRepository,
                                   SubscriberRepository subscriberRepository,
                                   UserRepository userRepository) {
        this.connectedServiceRepository = connectedServiceRepository;
        this.tariffPlanRepository = tariffPlanRepository;
        this.subscriberRepository = subscriberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Вспомогательный метод для поиска абонента по логину.
     */
    @Transactional(readOnly = true)
    protected Subscriber findSubscriberByLogin(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь '" + login + "' не найден."));
        return subscriberRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Абонент для пользователя '" + login + "' не найден."));
    }

    /**
     * Получает ТОЛЬКО АКТИВНЫЕ И ОЖИДАЮЩИЕ услуги для абонента.
     */
    @Transactional(readOnly = true)
    public Set<Integer> getActiveOrPendingTariffPlanIds(String login) {
        try {
            Subscriber subscriber = findSubscriberByLogin(login);
            List<ConnectedService> services = connectedServiceRepository
                    .findBySubscriberAndStatusIn(subscriber, List.of(ServiceStatus.ACTIVE, ServiceStatus.PENDING));
            return services.stream()
                    .map(cs -> cs.getTariffPlan().getId())
                    .collect(Collectors.toSet());
        } catch (EntityNotFoundException | UsernameNotFoundException e) {
            return Set.of();
        }
    }

    /**
     * Обрабатывает подключение услуги абонентом.
     */
    @Transactional
    public void requestServiceConnection(String login, int tariffPlanId) {
        Subscriber subscriber = findSubscriberByLogin(login); // Находим абонента
        TariffPlan tariffPlan = tariffPlanRepository.findById(tariffPlanId)
                .orElseThrow(() -> new EntityNotFoundException("Тарифный план с ID " + tariffPlanId + " не найден."));

        boolean alreadyConnectedOrPending = connectedServiceRepository
                .existsBySubscriberAndTariffPlanAndStatusIn(subscriber, tariffPlan, List.of(ServiceStatus.ACTIVE, ServiceStatus.PENDING));
        if (alreadyConnectedOrPending) {
            throw new IllegalStateException("Услуга '" + tariffPlan.getName() + "' уже подключена или ожидает подключения.");
        }

        ConnectedService newServiceRequest = new ConnectedService();
        newServiceRequest.setSubscriber(subscriber);
        newServiceRequest.setTariffPlan(tariffPlan);
        newServiceRequest.setStartDate(LocalDate.now());
        newServiceRequest.setStatus(ServiceStatus.PENDING);

        connectedServiceRepository.save(newServiceRequest);
    }

    /**
     * Получает ВСЕ услуги (всех статусов) для абонента.
     */
    @Transactional(readOnly = true)
    public List<ConnectedService> getAllServicesForSubscriber(String login) {
        Subscriber subscriber = findSubscriberByLogin(login);
        return connectedServiceRepository.findBySubscriberOrderByStatusAscStartDateDesc(subscriber);
    }

    /**
     * Получает ТОЛЬКО АКТИВНЫЕ услуги для абонента.
     */
    @Transactional(readOnly = true)
    public List<ConnectedService> getActiveServicesForSubscriber(String login) {
        try {
            Subscriber subscriber = findSubscriberByLogin(login);
            return connectedServiceRepository.findBySubscriberAndStatus(subscriber, ServiceStatus.ACTIVE);
        } catch (EntityNotFoundException | UsernameNotFoundException e) {
            return List.of();
        }
    }

    /**
     * Рассчитывает общую ежемесячную стоимость активных услуг для абонента.
     */
    @Transactional(readOnly = true)
    public double calculateTotalMonthlyCost(String login) {
        List<ConnectedService> activeServices = getActiveServicesForSubscriber(login);
        return activeServices.stream()
                .mapToDouble(service -> service.getTariffPlan().getMonthlyPayment().doubleValue())
                .sum();
    }

    /**
     * Обрабатывает отключение услуги абонентом.
     */
    @Transactional
    public void disconnectService(String login, int serviceId) {
        Subscriber subscriber = findSubscriberByLogin(login);

        ConnectedService service = connectedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Услуга с ID " + serviceId + " не найдена."));

        if (!Objects.equals(service.getSubscriber().getId(), subscriber.getId())) {
            throw new IllegalStateException("Вы не можете отключить эту услугу.");
        }

        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new IllegalStateException("Отключить можно только активную услугу. Текущий статус: " + service.getStatus());
        }

        service.setStatus(ServiceStatus.INACTIVE);
        connectedServiceRepository.save(service);
    }
}