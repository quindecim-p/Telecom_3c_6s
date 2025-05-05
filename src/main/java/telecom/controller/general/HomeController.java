package telecom.controller.general;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import telecom.entity.TariffPlan;
import telecom.enums.TariffType;
import telecom.service.user.TariffPlanService;

import java.util.List;

@Controller
public class HomeController {

    private final TariffPlanService tariffPlanService;

    @Autowired
    public HomeController(TariffPlanService tariffPlanService) {
        this.tariffPlanService = tariffPlanService;
    }

    /**
     * Отображает главную страницу.
     */
    @GetMapping({"/", "/home"})
    public String homePage(Model model) {
        try {
            List<TariffPlan> recommendedTariffs = tariffPlanService.findTariffsByType(TariffType.MIX);
            model.addAttribute("recommendedTariffs", recommendedTariffs);
            model.addAttribute("tariffTypes", TariffType.values());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Не удалось загрузить рекомендуемые тарифы.");
            model.addAttribute("recommendedTariffs", List.of());
            model.addAttribute("tariffTypes", TariffType.values());
        }
        return "general/home";
    }
}