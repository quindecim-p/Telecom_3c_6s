package telecom.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.entity.TariffPlan;
import telecom.enums.TariffType;
import telecom.repository.TariffPlanRepository;

import java.util.List;

@Service
public class TariffPlanService {

    private final TariffPlanRepository tariffPlanRepository;


    @Autowired
    public TariffPlanService(TariffPlanRepository tariffPlanRepository) {
        this.tariffPlanRepository = tariffPlanRepository;
    }

    /**
     * Возвращает список всех доступных тарифных планов.
     */
    public List<TariffPlan> getAllAvailableTariffs() {
        return tariffPlanRepository.findAll();
    }

    /**
     * Возвращает список тарифных планов по заданному типу.
     */
    @Transactional(readOnly = true)
    public List<TariffPlan> findTariffsByType(TariffType type) {
        return tariffPlanRepository.findByType(type);
    }
}