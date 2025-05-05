package telecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import telecom.entity.TariffPlan;
import telecom.enums.TariffType;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffPlanRepository extends JpaRepository<TariffPlan, Integer>, JpaSpecificationExecutor<TariffPlan> {

    List<TariffPlan> findByType(TariffType type);

    Optional<TariffPlan> findByName(String name);

}