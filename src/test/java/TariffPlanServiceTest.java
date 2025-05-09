import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import telecom.entity.TariffPlan;
import telecom.enums.TariffType;
import telecom.repository.TariffPlanRepository;
import telecom.service.user.TariffPlanService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TariffPlanServiceTest {

    @Mock
    private TariffPlanRepository tariffPlanRepository;

    @InjectMocks
    private TariffPlanService tariffPlanService;

    private TariffPlan plan1;
    private TariffPlan plan2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        plan1 = new TariffPlan();
        plan1.setId(1);
        plan1.setName("Plan 1");
        plan1.setType(TariffType.INTERNET);

        plan2 = new TariffPlan();
        plan2.setId(2);
        plan2.setName("Plan 2");
        plan2.setType(TariffType.MOBILE);
    }

    @Test
    void getAllAvailableTariffs_shouldReturnAllTariffs() {
        when(tariffPlanRepository.findAll()).thenReturn(List.of(plan1, plan2));

        List<TariffPlan> result = tariffPlanService.getAllAvailableTariffs();

        assertEquals(2, result.size());
        assertEquals("Plan 1", result.get(0).getName());
        assertEquals("Plan 2", result.get(1).getName());
    }

    @Test
    void getAllAvailableTariffs_shouldReturnEmptyList_whenNoTariffs() {
        when(tariffPlanRepository.findAll()).thenReturn(List.of());

        List<TariffPlan> result = tariffPlanService.getAllAvailableTariffs();

        assertEquals(0, result.size());
    }

    @Test
    void findTariffsByType_shouldReturnTariffsOfGivenType() {
        when(tariffPlanRepository.findByType(TariffType.INTERNET)).thenReturn(List.of(plan1));

        List<TariffPlan> result = tariffPlanService.findTariffsByType(TariffType.INTERNET);

        assertEquals(1, result.size());
        assertEquals(TariffType.INTERNET, result.get(0).getType());
    }

    @Test
    void findTariffsByType_shouldReturnEmptyList_whenNoTariffsOfType() {
        when(tariffPlanRepository.findByType(TariffType.TV)).thenReturn(List.of());

        List<TariffPlan> result = tariffPlanService.findTariffsByType(TariffType.TV);

        assertEquals(0, result.size());
    }

    @Test
    void findTariffsByType_shouldHandleNullTypeGracefully() {
        when(tariffPlanRepository.findByType(null)).thenReturn(List.of());

        List<TariffPlan> result = tariffPlanService.findTariffsByType(null);

        assertEquals(0, result.size());
    }
}
