package telecom.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import telecom.service.system.BillingService;

@Component
public class BillingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BillingScheduler.class);

    private final BillingService billingService;

    @Autowired
    public BillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Запускает ежедневный процесс списания средств.
     * Cron: "0 0 1 * * ?" - запускать каждый день в 1:00 ночи.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyBillingTask() {
        try {
            billingService.processDailyBilling();
        } catch (Exception e) {
            logger.error("Ошибка при выполнении ежедневного биллинга", e);
        }
    }
}