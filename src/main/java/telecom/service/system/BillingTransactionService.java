package telecom.service.system;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import telecom.entity.BillingAccount;
import telecom.entity.ConnectedService;
import telecom.entity.Subscriber;
import telecom.enums.ServiceStatus;
import telecom.repository.BillingAccountRepository;
import telecom.repository.ConnectedServiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingTransactionService {

    private final BillingAccountRepository billingAccountRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @Autowired
    public BillingTransactionService(BillingAccountRepository billingAccountRepository,
                                     ConnectedServiceRepository connectedServiceRepository) {
        this.billingAccountRepository = billingAccountRepository;
        this.connectedServiceRepository = connectedServiceRepository;
    }

    /**
     * Обрабатывает списание для ОДНОГО абонента в ОТДЕЛЬНОЙ транзакции.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processBillingForSubscriberWithTx(Subscriber subscriber, List<ConnectedService> services, LocalDate today) {
        int subscriberId = subscriber.getId();

        BillingAccount account = billingAccountRepository.findByIdWithPessimisticLock(subscriberId)
                .orElseThrow(() -> new EntityNotFoundException("BillingAccount not found for subscriber ID: " + subscriberId));

        BigDecimal totalCharge = services.stream()
                .map(service -> service.getTariffPlan() != null ? service.getTariffPlan().getMonthlyPayment() : BigDecimal.ZERO)
                .filter(payment -> payment != null && payment.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        if (totalCharge.compareTo(BigDecimal.ZERO) <= 0) {
            updateNextBillingDate(services, today);
            return;
        }

        BigDecimal currentBalance = account.getBalance();

        if (currentBalance.compareTo(totalCharge) >= 0) {
            account.setBalance(currentBalance.subtract(totalCharge));
            billingAccountRepository.save(account);
            updateNextBillingDate(services, today);
        } else {

            List<ConnectedService> deactivatedServices = new ArrayList<>();
            for (ConnectedService service : services) {
                if (service.getStatus() == ServiceStatus.ACTIVE) {
                    service.setStatus(ServiceStatus.INACTIVE);
                    deactivatedServices.add(service);
                }
            }
            if (!deactivatedServices.isEmpty()) {
                connectedServiceRepository.saveAll(deactivatedServices);
            }
        }
    }

    /**
     * Вспомогательный метод для обновления даты
     */
    private void updateNextBillingDate(List<ConnectedService> services, LocalDate billingDate) {
        LocalDate nextMonth = billingDate.plusMonths(1);
        List<ConnectedService> updatedServices = new ArrayList<>();
        for (ConnectedService service : services) {
            if (service.getNextBillingDate() != null && !service.getNextBillingDate().isAfter(billingDate)) {
                service.setNextBillingDate(nextMonth);
                updatedServices.add(service);
            }
        }
        if (!updatedServices.isEmpty()) {
            connectedServiceRepository.saveAll(updatedServices);
        }
    }
}