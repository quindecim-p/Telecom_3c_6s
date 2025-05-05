package telecom.service.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import telecom.entity.ConnectedService;
import telecom.entity.Subscriber;
import telecom.enums.ServiceStatus;
import telecom.repository.ConnectedServiceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final ConnectedServiceRepository connectedServiceRepository;
    private final BillingTransactionService billingTransactionService;

    @Autowired
    public BillingService(ConnectedServiceRepository connectedServiceRepository,
                          BillingTransactionService billingTransactionService) {
        this.connectedServiceRepository = connectedServiceRepository;
        this.billingTransactionService = billingTransactionService;
    }

    /**
     * Основной метод, запускаемый планировщиком.
     * Обрабатывает списание для всех услуг, у которых подошла дата.
     */
    public void processDailyBilling() {
        LocalDate today = LocalDate.now();

        List<ConnectedService> servicesToBill = connectedServiceRepository
                .findByStatusAndNextBillingDateLessThanEqual(ServiceStatus.ACTIVE, today);

        if (servicesToBill.isEmpty()) {
            return;
        }

        Map<Subscriber, List<ConnectedService>> servicesBySubscriber = servicesToBill.stream()
                .filter(cs -> cs.getSubscriber() != null)
                .collect(Collectors.groupingBy(ConnectedService::getSubscriber));

        for (Map.Entry<Subscriber, List<ConnectedService>> entry : servicesBySubscriber.entrySet()) {
            Subscriber subscriber = entry.getKey();
            List<ConnectedService> subscriberServices = entry.getValue();
            try {
                billingTransactionService.processBillingForSubscriberWithTx(subscriber, subscriberServices, today);
            } catch (Exception e) {
                System.err.println("Ошибка при обработке списания для абонента ID " +
                        subscriber.getId() + ": " + e.getMessage());
            }
        }
    }

}