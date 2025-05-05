package telecom.service.admin;

import telecom.entity.TariffPlan;
import telecom.enums.TariffType;
import telecom.repository.ConnectedServiceRepository;
import telecom.repository.TariffPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TariffsService {

    private final TariffPlanRepository tariffPlanRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @Autowired
    public TariffsService(TariffPlanRepository tariffPlanRepository,
                         ConnectedServiceRepository connectedServiceRepository) {
        this.tariffPlanRepository = tariffPlanRepository;
        this.connectedServiceRepository = connectedServiceRepository;
    }

    /**
     * Находит тарифы с учетом фильтров.
     */
    @Transactional(readOnly = true)
    public List<TariffPlan> findTariffsWithFilter(String nameKeyword, TariffType tariffType, Double minPrice, Double maxPrice) {

        Specification<TariffPlan> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(nameKeyword)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + nameKeyword.toLowerCase() + "%"));
            }

            if (tariffType != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), tariffType)); // Используем поле "type"
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("monthlyPayment"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("monthlyPayment"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return tariffPlanRepository.findAll(spec);
    }

    /**
     * Находит тариф по id.
     */
    @Transactional(readOnly = true)
    public Optional<TariffPlan> findTariffById(int id) {
        return tariffPlanRepository.findById(id);
    }

    /**
     * Сохраняет новый тариф.
     */
    @Transactional
    public void saveTariff(TariffPlan tariffPlan) {

        Optional<TariffPlan> existingTariffOpt = tariffPlanRepository.findByName(tariffPlan.getName());

        if (existingTariffOpt.isPresent()) {
            TariffPlan existingTariff = existingTariffOpt.get();
            boolean isNewTariff = tariffPlan.getId() == 0;
            boolean nameBelongsToAnotherTariff = !isNewTariff && existingTariff.getId() != tariffPlan.getId();

            if (isNewTariff || nameBelongsToAnotherTariff) {
                throw new IllegalArgumentException("Тариф с названием '" + tariffPlan.getName() + "' уже существует.");
            }
        }

        tariffPlanRepository.save(tariffPlan);
    }

    /**
     * Удаляет тариф.
     */
    @Transactional
    public void deleteTariff(int id) throws EntityNotFoundException, DataIntegrityViolationException {
        TariffPlan tariffPlan = tariffPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тариф с ID " + id + " не найден."));

        long usageCount = connectedServiceRepository.countByTariffPlan(tariffPlan);
        if (usageCount > 0) {
            throw new DataIntegrityViolationException("Невозможно удалить тариф '" + tariffPlan.getName() + "', так как он используется в " + usageCount + " подключенных услугах.");
        }

        tariffPlanRepository.deleteById(id);
    }
}